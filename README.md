# Liepa Recognition Engine

android_atpazintuvo_variklis is a minimalistic android app. It shows how to use PocketSphinx and Liepa Recognition Models on Android. 

This demonstration is based on https://github.com/cmusphinx/pocketsphinx-android-demo. This demo uses simplified scenario. Also Kotlin language is used instead of java. See for details http://cmusphinx.sourceforge.net/wiki/tutorialandroid for other PocketSphinx details. 

## Directory stucture

* sphinxmodels - Liepa Recognition Models: acoustic model, sample recognition grammar and dictionary for it. **Those files must be changed** based on problem that's needs to be resolve.
* aars - pocketshphinx recognition binaries and Java API wrapper. 
* apps - main android class that shows how to use recognition engine.

## Recogniton Engine Usage

Review: [LiepaSphinxRecognitionActivity.kt](https://github.com/liepa-project/android_atpazintuvo_variklis/blob/master/app/src/main/kotlin/lt/vu/liepa/atpazintuvas/LiepaSphinxRecognitionActivity.kt)
 * How to initialize recognizer?
   * ```onResume(...)``` - shows recognizer initialisation sequence.
   * ```assets.syncAssets()``` - ensures that Recognition Models are up to date(no new files after update or so).
   * ```SpeechRecognizerSetup.defaultSetup().setAcousticModel(...).setDictionary(...).recognizer``` - creates recognizer instance with accoustic model and dictionary.
   * ```recognizer.addListener(...)``` - add possiblity react on recognition events.
   * ```recognizer.addGrammarSearch(...)``` - adds addiotnal recognition grammar.
 * How to use recognizer?
   * ```recognizer.startListening(...)``` - start recording audio from microphine and send bytes for recognition
   * ```recognizer.stop()``` - stop recording and recognition coroutine
 * How to consume recognition results?
   * Implement ```edu.cmu.pocketsphinx.RecognitionListener``` java interface allows React on recognition events. see javadoc of LiepaSphinxRecognitionActivity.kt for more details.

## For iOS

sphinxmodels(Liepa Recognition Models: acoustic model, sample recognition grammar and dictionary for it) **should** be used in same way as for android, but API calls and usages might be different. 

Please, find more info here:

* https://github.com/tryolabs/TLSphinx (https://tryolabs.com/blog/2015/06/15/tlsphinx-automatic-speech-recognition-asr-in-swift/)
* https://www.politepix.com/openears/
* https://github.com/cmusphinx/pocketsphinx-ios-demo
