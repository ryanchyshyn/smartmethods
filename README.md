# Smart Methods
Smart Methods Java code annotations.

Smart Method is an object that holds method reference altogether with its parameters. The key feature is that Smart Method will call the method automatically once all parameters are set thereby ensuring method is called with valid values.

## Usage
### Java
Modify you module **build.gradle** file:
```Groovy
repositories {
    jcenter()
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

As you see **doSomeWork** object declaration is a bit fancy. Also it requires you to correctly call your target method inside **MethodDelegate**.

#### Java 1.8
Hopefully if you target Java 1.8 you can simplify Smart Method declaration:
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

Of cource there is no much sense to declare SmartMethods for methods with only one parameter. But using it on methods with multiple parameters can significantly improve code quality:
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
    jcenter()
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

### Better sample

Here is the sample of Android Activity that has one button. Clicking on it will send NTP request to get current time and once the response is received, pronounce it using standard Android TTS engine.

<details><summary>Code</summary>
<p>

```Kotlin
class MainActivity : AppCompatActivity() {

	// region Smart Methods
	private val speakTimeSmartMethod = MainActivity_SpeakTime(this::speakTime)
	// endregion

	// this field gets non null value only for period of service connection
	private var tts: TextToSpeech? = null
	private var getTimeAsyncTask: GetTimeAsyncTask? = null

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main)

		btnGetTime.setOnClickListener {
			initTts()
			getTime()
		}
	}

	override fun onDestroy() {
		tts?.shutdown()

		speakTimeSmartMethod.isEnabled = false
		speakTimeSmartMethod.tts?.shutdown()
		speakTimeSmartMethod.clear()

		getTimeAsyncTask?.cancel(true)

		super.onDestroy()
	}

	@SmartMethod
	private fun speakTime(tts: TextToSpeech, date: Date) {
		tts.speak(date.toString(), TextToSpeech.QUEUE_FLUSH, null, "")
	}

	private fun initTts() {
		if ((tts != null) || speakTimeSmartMethod.isTtsSet) return

		tts = TextToSpeech(this, TextToSpeech.OnInitListener { status: Int ->
			if (status == TextToSpeech.SUCCESS) speakTimeSmartMethod.tts = tts
			else Toast.makeText(this@MainActivity, "Failed to init TTS. Status code: $status", Toast.LENGTH_SHORT).show()
			tts = null
		})
	}

	private fun getTime() {
		if (getTimeAsyncTask != null) return

		getTimeAsyncTask = object: GetTimeAsyncTask() {
			override fun onPostExecute(result: Result<Date, Exception>) {
				getTimeAsyncTask = null

				result.fold(
						{ value -> txtTime.text = "Time: $value"},
						{ error -> txtTime.text = "Failed to get time. Error: $error" })

				speakTimeSmartMethod.date = result.component1()
			}
		}
		getTimeAsyncTask?.execute()
	}
}
```

</p>
</details>

## Documentation
### Annotation parameters:
```Java
boolean oneShot() default false;
boolean threadSafe() default false;
boolean enabled() default true;
boolean debug() default false;
String customClassName() default "";
String customPackageName() default "";
```
* oneShot - if `true` all parameters will be cleared after method calling
* threadSafe - `true` makes all methods thread safe
* enabled - initial `enabled` state of Smart Method object
* debug - initial `debug` state of Smart Method object
* customClassName - custom class name. If not specified the class name is constructed in form `OuterClass_MethodName`
* customPackageName - custom package name. If not specified the package name is the same as outer class name

### Smart Method class methods:
```Java
/**
 * Get 'enabled' state.
 * @return 'enabled' value. */
boolean isEnabled();

/**
 * Set 'enabled' state.
 * @param enabled new value.
 * @return new state. */
boolean setEnabled(boolean enabled);

/**
 * Get 'debug' state.
 * @return 'debug' value. */
boolean isDebug();

/**
 * Set 'debug' state.
 * @param debug new value.
 * @return new state. */
boolean setDebug(boolean debug);

/**
 * Call method if all parameters are set.
 * @return true when method was called. */
boolean fire();

/**
 * Check whether all parameters are set.
 * @return true when all parameters are set. */
boolean areParametersSet();

/**
 * Clear all parameter. */
void clear();


/**
 * Get Xxx parameter.
 * @return Xxx value. */
T getXxx();

/**
 * Set Xxx parameter.
 * @param Xxx method parameter.
 * @return true when the method was fired. */
boolean setXxx(T Xxx);

/**
 * Set Xxx parameter but don't call the method.
 * @param Xxx method parameter.
 * @return new value. */
T assignXxx(T Xxx);

/**
 * Check whether Xxx parameter is set.
 * @return true when parameter is set. */
boolean isXxxSet();

/**
 * Clear Xxx parameter. */
void clearXxx();
```

`getXxx, setXxx, assignXxx, isXxxSet, clearXxx` methods corresponds fo `Xxx` parameter. Every Smart Method parameter has such set of methods.
