# Smart Methods
Smart Methods Java code annotations.

Smart Method is an object that holds method reference altogether with its parameters. The key feature is that Smart Method will call the method automatically once all parameters are set thereby ensuring method is called with valid values.

## Usage
### Java
Modify you module **build.gradle** file:
```Groovy
repositories {
    maven {
        url "https://dl.bintray.com/ryanchyshyn/smartmethods/"
    }
}

dependencies {
    implementation 'com.yanchyshyn:smartmethods-annotation:1.0.0'
    annotationProcessor 'com.yanchyshyn:smartmethods-processor:1.0.0'
}
```

Now you can annotate any method with **SmartMethod** annotation:

```Java
import com.yanchyshyn.smartmethods.SmartMethod;
...

public class MyClass {
	@SmartMethod
	private void doSomeWork(int intParam) {
		// TODO
	}
}
```

#### Java 1.7
Once you build your project a SmartMethod class on current package with the name **MyClass_DoSomeWork** will be auto-generated.
You can use it in the following way:
```Java
public class MyClass {
	private final MyClass_DoSomeWork doSomeWork = new MyClass_DoSomeWork(new MyClass_DoSomeWork.MethodDelegate() {
		@Override
		public void call(int intParam) {
			doSomeWork(intParam);
		}
	});

	public void testSomeWork() {
		// since doSomeWork has only one parameter, then setting intParam will cause to call doSomeWork method
		doSomeWork.setIntParam(0x1234);
	}

	@SmartMethod
	private void doSomeWork(int intParam) {
		System.out.println("doSomeWork: " + intParam);
	}
}
```

Unfortunately as you see **doSomeWork** object declaration is a bit fancy. Also it requires you to correctly call your target method inside **MethodDelegate**.

#### Java 1.8
Hopefully if you target Java 1.8 you can simplify Smart Method declaration to the following:
```Java
public class MyClass18 {
	private final MyClass_DoSomeWork doSomeWork = new MyClass_DoSomeWork(this::doSomeWork);

	public void testSomeWork() {
		// since doSomeWork has only one parameter, then setting intParam will cause to call doSomeWork method
		doSomeWork.setIntParam(0x1234);
	}

	@SmartMethod
	private void doSomeWork(int intParam) {
		System.out.println("doSomeWork: " + intParam);
	}
}
```

Of cource there is no much sense to use Smart Methods for methods with only one parameter. But using it on methods with multiple parameters can significantly improve code quality:
```Java
public class MyFragment extends Fragment {
	private final MyFragment_DoSomeWork doSomeWork = new MyFragment_DoSomeWork(this::doSomeWork);

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View ret = inflater.inflate(R.layout.my_fragment, container, false);
		doSomeWork.setView(ret.findViewById(R.id.my_view));
		return ret;
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		doSomeWork.setActivity(activity);
	}

	@SmartMethod
	private void doSomeWork(View view, Activity activity) {
	}
}
```

### Kotlin
Of course you can use it with Kotlin.

Firstly modify you module **build.gradle** file:
```Groovy
apply plugin: 'kotlin-kapt'

repositories {
    maven {
        url "https://dl.bintray.com/ryanchyshyn/smartmethods/"
    }
}

dependencies {
    implementation 'com.yanchyshyn:smartmethods-annotation:1.0.0'
    kapt 'com.yanchyshyn:smartmethods-processor:1.0.0'
}
```

```Kotlin
class MyClassKotlin {
	private val doSomeWorkSM = MyClassKotlin_DoSomeWork(::doSomeWork)

	fun testSomeWork() {
		// since doSomeWork has only one parameter, then setting intParam will cause to call doSomeWork method
		doSomeWorkSM.intParam = 0x1234
	}

	@SmartMethod
	private fun doSomeWork(intParam: Int) {
		println("doSomeWork: $intParam")
	}
}
```

Note that SmartMethod object in this case is named **doSomeWorkSM**. This is because Kotlin does not allows to create objects with the same name as existing methods. Of course you can rename it to anything else.
