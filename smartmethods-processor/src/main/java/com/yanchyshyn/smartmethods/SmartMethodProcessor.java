package com.yanchyshyn.smartmethods;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.google.common.base.Defaults;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.google.common.primitives.Primitives;
import com.squareup.javapoet.*;
import org.jetbrains.annotations.NotNull;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeKind;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class SmartMethodProcessor extends AbstractProcessor {
	
	private static final String IS_SET_SUFFIX = "IsSet";

	private Filer filer;
	private Messager messager;
	private Elements elements;

	@Override
	public synchronized void init(ProcessingEnvironment processingEnvironment) {
		super.init(processingEnvironment);
		filer = processingEnvironment.getFiler();
		messager = processingEnvironment.getMessager();
		elements = processingEnvironment.getElementUtils();
	}

	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment env) {
		for (Element element : env.getElementsAnnotatedWith(SmartMethod.class)) {
			if (element.getKind() != ElementKind.METHOD) {
				messager.printMessage(Diagnostic.Kind.ERROR, "Can be applied only to a method.");
				return true;
			}

			Element enclosingEl = element.getEnclosingElement();
			if (enclosingEl.getKind() != ElementKind.CLASS) {
				messager.printMessage(Diagnostic.Kind.WARNING, "SmartMethod element is located not in class. Skipping.", element);
				continue;
			}

			SmartMethod smartMethodAnn = element.getAnnotation(SmartMethod.class);
			boolean oneShot = smartMethodAnn.oneShot();
			boolean threadSafe = smartMethodAnn.threadSafe();
			boolean enabled = smartMethodAnn.enabled();
			boolean debug = smartMethodAnn.debug();
			String smartMethodClassName = smartMethodAnn.customClassName();
			String smartMethodClassPackageName = smartMethodAnn.customPackageName();

			TypeElement enclosingClassTypeEl = (TypeElement) enclosingEl;
			String methodName = element.getSimpleName().toString();
			ExecutableElement smartMethod = (ExecutableElement) element;
			if (smartMethodClassName.equals("")) smartMethodClassName = enclosingClassTypeEl.getSimpleName() + "_" + capitalize(methodName);
			if (smartMethod.getReturnType().getKind() != TypeKind.VOID) {
				messager.printMessage(Diagnostic.Kind.WARNING, "SmartMethod return value is not void. You can't handle it.", element);
			}

			List<ParameterSpec> methodParameters = getMethodParameters(smartMethod);
			if (methodParameters.isEmpty()) {
				messager.printMessage(Diagnostic.Kind.ERROR, String.format("Method '%s' should have at least one parameter.", methodName));
				return true;
			}

			// check parameter names for reserved values
			for (ParameterSpec ps : methodParameters) {
				if (ps.name.equals("enabled") || ps.name.equals("debug")) {
					messager.printMessage(Diagnostic.Kind.ERROR, String.format("'%s' parameter is reserved and can't ber used.", ps.name));
					return true;
				}
			}

			// MethodDelegate interface
			TypeSpec.Builder methodDelegateClassBuilder = TypeSpec
					.interfaceBuilder("MethodDelegate")
					.addModifiers(Modifier.PUBLIC);

			MethodSpec.Builder callMethod = MethodSpec
					.methodBuilder("call")
					.addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
					.addParameters(methodParameters);

			methodDelegateClassBuilder.addMethod(callMethod.build());

			TypeSpec methodDelegateClass = methodDelegateClassBuilder.build();


			// SmartMethod class
			TypeSpec.Builder smartMethodClassBuilder = TypeSpec
					.classBuilder(smartMethodClassName)
					.addModifiers(Modifier.PUBLIC);


			smartMethodClassBuilder.addType(methodDelegateClass);

			// methodDelegate field
			smartMethodClassBuilder.addField(ClassName.bestGuess(methodDelegateClass.name), "methodDelegate", Modifier.PRIVATE, Modifier.FINAL);

			// enabled field
			smartMethodClassBuilder.addField(FieldSpec.builder(Boolean.TYPE, "enabled", Modifier.PRIVATE).initializer(String.valueOf(enabled)).build());

			// debug field
			smartMethodClassBuilder.addField(FieldSpec.builder(Boolean.TYPE, "debug", Modifier.PRIVATE).initializer(String.valueOf(debug)).build());

			// smart method parameters fields
			for (ParameterSpec ps : methodParameters) {
				FieldSpec.Builder fieldSpecBuilder = FieldSpec.builder(ps.type, ps.name, Modifier.PRIVATE)
						.addAnnotations(ps.annotations);
				smartMethodClassBuilder.addField(fieldSpecBuilder.build());
				smartMethodClassBuilder.addField(Boolean.TYPE, ps.name + IS_SET_SUFFIX, Modifier.PRIVATE);
			}

			// SmartMethod ctor
			MethodSpec ctor = MethodSpec
					.constructorBuilder()
					.addModifiers(Modifier.PUBLIC)
					.addParameter(ClassName.bestGuess(methodDelegateClass.name), "methodDelegate")
					.addStatement("this.$N = $N", "methodDelegate", "methodDelegate")
					.build();
			smartMethodClassBuilder.addMethod(ctor);

			// enabled property
			smartMethodClassBuilder.addMethod(getIsEnabled(threadSafe));
			smartMethodClassBuilder.addMethod(getSetEnabled(threadSafe));

			// debug property
			smartMethodClassBuilder.addMethod(getIsDebug(threadSafe));
			smartMethodClassBuilder.addMethod(getSetDebug(threadSafe));

			// properties
			for (ParameterSpec ps : methodParameters) {
				smartMethodClassBuilder.addMethod(getPropertyGetter(threadSafe, ps));
				smartMethodClassBuilder.addMethod(getPropertySetter(threadSafe, ps));
				smartMethodClassBuilder.addMethod(getPropertyAssign(threadSafe, ps));
				smartMethodClassBuilder.addMethod(getIsPropertySet(threadSafe, ps));
				smartMethodClassBuilder.addMethod(getPropertyClear(threadSafe, ps));
			}

			// fire method
			smartMethodClassBuilder.addMethod(getFire(threadSafe, oneShot, methodParameters));

			// areParametersSet method
			smartMethodClassBuilder.addMethod(getAreParametersSet(threadSafe, methodParameters));

			// clear method
			smartMethodClassBuilder.addMethod(getClear(threadSafe, methodParameters));

			try {
				if (smartMethodClassPackageName.equals("")) {
					Element e = processingEnv.getTypeUtils().asElement(enclosingClassTypeEl.asType());
					PackageElement pkg = processingEnv.getElementUtils().getPackageOf(e);
					smartMethodClassPackageName = pkg.getQualifiedName().toString();
				}
				JavaFile.builder(smartMethodClassPackageName, smartMethodClassBuilder.build()).build().writeTo(filer);
			}
			catch (IOException e) {
				messager.printMessage(Diagnostic.Kind.ERROR, "Failed to write generated class. Error: " + e.toString());
			}
		}

		return true;
	}

	@Override
	public Set<String> getSupportedAnnotationTypes() {
		return ImmutableSet.of(SmartMethod.class.getCanonicalName());
	}

	@Override
	public SourceVersion getSupportedSourceVersion() {
		return SourceVersion.latestSupported();
	}

	private static MethodSpec getPropertyGetter(boolean threadSafe, ParameterSpec ps) {
		MethodSpec getter = MethodSpec
				.methodBuilder("get" + capitalize(ps.name))
				.addJavadoc("Get $N parameter.\n", ps.name)
				.addJavadoc("@return $N value.", ps.name)
				.addModifiers(Modifier.PUBLIC)
				.addAnnotations(ps.annotations)
				.returns(ps.type)
				.beginControlFlow(threadSafe ? "synchronized(this)" : "")
				.addStatement("return $N", ps.name)
				.endControlFlow()
				.build();

		return getter;
	}

	private static boolean isNullableParameterSpec(ParameterSpec ps) {
		for (AnnotationSpec ann : ps.annotations) {
			if (ann.type.toString().equals(Nullable.class.getCanonicalName())) return true;
			if (ann.type.toString().equals(org.jetbrains.annotations.Nullable.class.getCanonicalName())) return true;
			if (ann.type.toString().equals(NonNull.class.getCanonicalName())) return false;
			if (ann.type.toString().equals(NotNull.class.getCanonicalName())) return false;
		}
		return true;
	}

	private static MethodSpec getPropertySetter(boolean threadSafe, ParameterSpec ps) {
		MethodSpec.Builder setterBuilder = MethodSpec
				.methodBuilder("set" + capitalize(ps.name))
				.addJavadoc("Set $N parameter.\n", ps.name)
				.addJavadoc("@param $N method parameter.\n", ps.name)
				.addJavadoc("@return true when the method was fired.")
				.addModifiers(Modifier.PUBLIC)
				.returns(Boolean.TYPE)
				.addParameter(ps);

		if (ps.type.isPrimitive()) {
			setterBuilder
					.beginControlFlow(threadSafe ? "synchronized(this)" : "")
					.addStatement("if (!enabled) return false")
					.addStatement("if (isDebug()) System.err.println(\"set$N: \" + $N)", capitalize(ps.name), ps.name)
					.addStatement("this.$N = $N", ps.name, ps.name)
					.addStatement("this.$N = $N", ps.name + IS_SET_SUFFIX, "true")
					.endControlFlow()
					.addStatement("if (debug) System.err.println(\"set$N: \" + $N)", capitalize(ps.name), ps.name)
					.addStatement("return fire()");
		}
		else {
			boolean isNullable = isNullableParameterSpec(ps);
			if (isNullable) {
				setterBuilder
						.beginControlFlow("if ($N == null)", ps.name)
						.beginControlFlow(threadSafe ? "synchronized(this)" : "")
						.addStatement("if (!enabled) return false")
						.addStatement("this.$N = $N", ps.name, getTypeDefaultValue(ps.type))
						.addStatement("this.$N = $N", ps.name + IS_SET_SUFFIX, "true")
						.addStatement("if (isDebug()) System.err.println(\"set$N: \" + $N)", capitalize(ps.name), ps.name)
						.addStatement("return false")
						.endControlFlow()
						.endControlFlow()
						.beginControlFlow("else")
						.beginControlFlow(threadSafe ? "synchronized(this)" : "")
						.addStatement("if (!enabled) return false")
						.addStatement("this.$N = $N", ps.name, ps.name)
						.addStatement("this.$N = $N", ps.name + IS_SET_SUFFIX, "true")
						.addStatement("if (isDebug()) System.err.println(\"set$N: \" + $N)", capitalize(ps.name), ps.name)
						.endControlFlow()
						.addStatement("return fire()")
						.endControlFlow();
			}
			else {
				setterBuilder
						.beginControlFlow(threadSafe ? "synchronized(this)" : "")
						.addStatement("if (!enabled) return false")
						.addStatement("this.$N = $N", ps.name, ps.name)
						.addStatement("this.$N = $N", ps.name + IS_SET_SUFFIX, "true")
						.endControlFlow()
						.addStatement("return fire()");
			}
		}

		return setterBuilder.build();
	}

	private static MethodSpec getPropertyAssign(boolean threadSafe, ParameterSpec ps) {
		MethodSpec.Builder ret = MethodSpec
				.methodBuilder("assign" + capitalize(ps.name))
				.addJavadoc("Set $N parameter but don't call the method.\n", ps.name)
				.addJavadoc("@param $N method parameter.\n", ps.name)
				.addJavadoc("@return new value.")
				.addModifiers(Modifier.PUBLIC)
				.addAnnotations(ps.annotations)
				.returns(ps.type)
				.addParameter(ps)
				.beginControlFlow(threadSafe ? "synchronized(this)" : "")
				.addStatement("if (!enabled) return $N", ps.name)
				.addStatement("this.$N = $N", ps.name, ps.name)
				.addStatement("this.$N = $N", ps.name + IS_SET_SUFFIX, "true")
				.addStatement("if (isDebug()) System.err.println(\"assign$N: \" + $N)", capitalize(ps.name), ps.name)
				.endControlFlow()
				.addStatement("return $N", ps.name);

		return ret.build();
	}

	private static MethodSpec getIsPropertySet(boolean threadSafe, ParameterSpec ps) {
		MethodSpec ret = MethodSpec
				.methodBuilder("is" + capitalize(ps.name) + "Set")
				.addJavadoc("Check whether $N parameter is set.\n", ps.name)
				.addJavadoc("@return true when parameter is set.")
				.addModifiers(Modifier.PUBLIC)
				.returns(Boolean.TYPE)
				.beginControlFlow(threadSafe ? "synchronized(this)" : "")
				.addStatement("return $N", ps.name + IS_SET_SUFFIX)
				.endControlFlow()
				.build();

		return ret;
	}

	private static MethodSpec getPropertyClear(boolean threadSafe, ParameterSpec ps) {
		MethodSpec clear = MethodSpec
				.methodBuilder("clear" + capitalize(ps.name))
				.addJavadoc("Clear $N parameter.", ps.name)
				.addModifiers(Modifier.PUBLIC)
				.beginControlFlow(threadSafe ? "synchronized(this)" : "")
				.addStatement("if (!enabled) return")
				.addStatement("$N = $N", ps.name, getTypeDefaultValue(ps.type))
				.addStatement("$N = $N", ps.name + IS_SET_SUFFIX, "false")
				.addStatement("if (isDebug()) System.err.println(\"clear$N\")", capitalize(ps.name))
				.endControlFlow()
				.build();

		return clear;
	}

	private static MethodSpec getFire(boolean threadSafe, boolean oneShot, List<ParameterSpec> methodParameters) {
		MethodSpec ret = MethodSpec
				.methodBuilder("fire")
				.addJavadoc("Call method if all parameters are set.\n")
				.addJavadoc("@return true when method was called.")
				.addModifiers(Modifier.PUBLIC)
				.returns(Boolean.TYPE)
				.addStatement(getFieldsCopyDeclStatement(methodParameters))
				.beginControlFlow(threadSafe ? "synchronized(this)" : "")
				.addStatement("if (!enabled) return false")
				.addStatement(getFieldsCopyStatement(methodParameters))
				.endControlFlow()
				.beginControlFlow(getIfStatement(methodParameters))
				.addStatement("if (isDebug()) System.err.println(\"firing method\")")
				.addStatement(oneShot ? "clear()" : "")
				.addStatement(getCallDelegateStatement(methodParameters))
				.addStatement("return true")
				.endControlFlow()
				.addStatement("return false")
				.build();

		return ret;
	}

	private static MethodSpec getAreParametersSet(boolean threadSafe, List<ParameterSpec> methodParameters) {
		MethodSpec ret = MethodSpec
				.methodBuilder("areParametersSet")
				.addJavadoc("Check whether all parameters are set.\n")
				.addJavadoc("@return true when all parameters are set.")
				.addModifiers(Modifier.PUBLIC)
				.returns(Boolean.TYPE)
				.beginControlFlow(threadSafe ? "synchronized(this)" : "")
				.addStatement("return " + getCheckAllParamsStatement(methodParameters))
				.endControlFlow()
				.build();

		return ret;
	}

	private static MethodSpec getClear(boolean threadSafe, List<ParameterSpec> methodParameters) {
		MethodSpec.Builder ret = MethodSpec
				.methodBuilder("clear")
				.addJavadoc("Clear all parameter.")
				.addModifiers(Modifier.PUBLIC)
				.beginControlFlow(threadSafe ? "synchronized(this)" : "")
				.addStatement("if (!enabled) return");

		for (String statement : getResetStatements(methodParameters))
			ret.addStatement(statement);

		ret.addStatement("if (isDebug()) System.err.println(\"clear\")");
		ret.endControlFlow();

		return ret.build();
	}

	private static MethodSpec getIsEnabled(boolean threadSafe) {
		MethodSpec ret = MethodSpec
				.methodBuilder("isEnabled")
				.addJavadoc("Get 'enabled' state.\n")
				.addJavadoc("@return 'enabled' value.")
				.addModifiers(Modifier.PUBLIC)
				.returns(Boolean.TYPE)
				.beginControlFlow(threadSafe ? "synchronized(this)" : "")
				.addStatement("return enabled")
				.endControlFlow()
				.build();

		return ret;
	}

	private static MethodSpec getSetEnabled(boolean threadSafe) {
		MethodSpec ret = MethodSpec
				.methodBuilder("setEnabled")
				.addJavadoc("Set 'enabled' state.\n")
				.addJavadoc("@param enabled new value.\n")
				.addJavadoc("@return new state.")
				.addModifiers(Modifier.PUBLIC)
				.returns(Boolean.TYPE)
				.addParameter(Boolean.TYPE, "enabled")
				.addStatement("if (isDebug()) System.err.println(\"setEnabled: \" + enabled)")
				.beginControlFlow(threadSafe ? "synchronized(this)" : "")
				.addStatement("this.enabled = enabled")
				.endControlFlow()
				.addStatement("return enabled")
				.build();

		return ret;
	}

	private static MethodSpec getIsDebug(boolean threadSafe) {
		MethodSpec ret = MethodSpec
				.methodBuilder("isDebug")
				.addJavadoc("Get 'debug' state.\n")
				.addJavadoc("@return 'debug' value.")
				.addModifiers(Modifier.PUBLIC)
				.returns(Boolean.TYPE)
				.beginControlFlow(threadSafe ? "synchronized(this)" : "")
				.addStatement("return debug")
				.endControlFlow()
				.build();

		return ret;
	}

	private static MethodSpec getSetDebug(boolean threadSafe) {
		MethodSpec ret = MethodSpec
				.methodBuilder("setDebug")
				.addJavadoc("Set 'debug' state.\n")
				.addJavadoc("@param debug new value.\n")
				.addJavadoc("@return new state.")
				.addModifiers(Modifier.PUBLIC)
				.returns(Boolean.TYPE)
				.addParameter(Boolean.TYPE, "debug")
				.addStatement("boolean log = false")
				.beginControlFlow(threadSafe ? "synchronized(this)" : "")
				.addStatement("log = debug || this.debug")
				.addStatement("this.debug = debug")
				.endControlFlow()
				.addStatement("if (log) System.err.println(\"setDebug: \" + debug)")
				.addStatement("return debug")
				.build();

		return ret;
	}

	private static List<ParameterSpec> getMethodParameters(ExecutableElement method) {
		List<ParameterSpec> ret = new ArrayList<>(method.getParameters().size());

		for (VariableElement varEl : method.getParameters()) {
			ParameterSpec.Builder ps = ParameterSpec.builder(TypeName.get(varEl.asType()), varEl.getSimpleName().toString());

			Annotation supportNullableAnnot = varEl.getAnnotation(Nullable.class);
			if (supportNullableAnnot != null) {
				ps.addAnnotation(Nullable.class);
			}

			Annotation jetbrainsNullableAnnot = varEl.getAnnotation(org.jetbrains.annotations.Nullable.class);
			if (jetbrainsNullableAnnot != null) {
				ps.addAnnotation(org.jetbrains.annotations.Nullable.class);
			}

			Annotation supportNonNullAnnot = varEl.getAnnotation(NonNull.class);
			if (supportNonNullAnnot != null) {
				ps.addAnnotation(NonNull.class);
			}

			Annotation jetbrainsNotNullAnnot = varEl.getAnnotation(NotNull.class);
			if (jetbrainsNotNullAnnot != null) {
				ps.addAnnotation(NotNull.class);
			}

			ret.add(ps.build());
		}

		return ret;
	}

	private static String capitalize(String str) {
		if ((str == null) || str.isEmpty()) return str;
		return str.substring(0, 1).toUpperCase() + str.substring(1);
	}

	private static String getFieldsCopyDeclStatement(List<ParameterSpec> psList) {
		StringBuilder ret = new StringBuilder();
		for (ParameterSpec ps : psList) {
			ret.append(ps.type.toString() + " " + ps.name + "_; ");
			ret.append("boolean " + ps.name + IS_SET_SUFFIX + "_; ");
		}
		return ret.toString();
	}

	private static String getFieldsCopyStatement(List<ParameterSpec> psList) {
		StringBuilder ret = new StringBuilder();
		for (ParameterSpec ps : psList) {
			ret.append(ps.name + "_ = " + ps.name + "; ");
			ret.append(ps.name + IS_SET_SUFFIX + "_ = " + ps.name + IS_SET_SUFFIX + "; ");
		}
		return ret.toString();
	}

	private static String getIfStatement(List<ParameterSpec> psList) {
		StringBuilder ret = new StringBuilder();
		ret.append("if ((methodDelegate != null)");
		for (ParameterSpec ps : psList) {
			ret.append(" && " + ps.name + IS_SET_SUFFIX + "_");
		}
		ret.append(")");
		return ret.toString();
	}

	private static String getCheckAllParamsStatement(List<ParameterSpec> psList) {
		StringBuilder ret = new StringBuilder();
		boolean first = true;
		for (ParameterSpec ps : psList) {
			if (first) first = false;
			else ret.append(" && ");
			ret.append(ps.name + IS_SET_SUFFIX);
		}
		return ret.toString();
	}

	private static String getCallDelegateStatement(List<ParameterSpec> psList) {
		StringBuilder ret = new StringBuilder();
		ret.append("methodDelegate.call(");
		boolean first = true;
		for (ParameterSpec ps : psList) {
			if (first) first = false;
			else ret.append(", ");
			ret.append(ps.name + "_");
		}
		ret.append(")");
		return ret.toString();
	}

	private static List<String> getResetStatements(List<ParameterSpec> psList) {
		List<String> ret = new ArrayList<>();

		for (ParameterSpec ps : psList) {
			ret.add(ps.name + " = " + getTypeDefaultValue(ps.type));
			ret.add(ps.name + "IsSet = false");
		}

		return ret;
	}

	/**
	 *
	 * @param type
	 * @return the default primitive value as a String.  Returns null if unable to determine default value
	 */
	private static String getTypeDefaultValue(TypeName type) {
		String ret = "null";
		try {
			if (type.isPrimitive()) {
				Class<?> primitiveClass = Primitives.unwrap(Class.forName(type.box().toString()));
				if (primitiveClass != null) {
					Object defaultValue = Defaults.defaultValue(primitiveClass);
					if (defaultValue != null) {
						ret = defaultValue.toString();
						if (!Strings.isNullOrEmpty(ret)) {
							switch (type.toString()) {
								case "double":
									ret = ret + "d";
									break;
								case "float":
									ret = ret + "f";
									break;
								case "long":
									ret = ret + "L";
									break;
								case "char":
									//ret = "'" + ret + "'";
									ret = "'\\0'";
									break;
							}
						}
					}
				}
			}
		} catch (ClassNotFoundException ignored) {
			//Swallow and return null
		}

		return ret;
	}

}
