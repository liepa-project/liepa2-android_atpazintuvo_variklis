/* ====================================================================
Copyright (c) 2013-2019, Vilnius University Institute of Mathematics and Informatics
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

* Redistributions of source code must retain the above copyright notice, this
  list of conditions and the following disclaimer.

* Redistributions in binary form must reproduce the above copyright notice,
  this list of conditions and the following disclaimer in the documentation
  and/or other materials provided with the distribution.

This work was supported in part by funding from the Europe Union.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

 * ====================================================================*/
package lt.vu.liepa.atpazintuvas

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import android.view.WindowManager
import edu.cmu.pocketsphinx.*
import kotlinx.android.synthetic.main.activity_atpazintuvas_main.*
import kotlinx.coroutines.*
import org.jetbrains.anko.longToast
import java.io.File


private const val PERMISSIONS_REQUEST_RECORD_AUDIO = 1
private const val TAG = "LIEPA_SPHINX"
private const val GRAMMAR = "ADDRESS_PHRASES"
//private const val ACOUSTIC_ID= "FZ1.3"
//private const val ACOUSTIC_ID= "L2_g5_ptm-20190723"
private const val ACOUSTIC_ID= "L2_g5_ptm-20201015"
private const val ACOUSTIC_MODEL_PATH = "models/$ACOUSTIC_ID/acoustic"
private const val DICTIONARY_FILE = "models/$ACOUSTIC_ID/language/adr_frazės.dict"

/**
 * Main activity class to load recognition engine and respond on recognition events.
 * Base on https://github.com/cmusphinx/pocketsphinx-android-demo
 * @author Mindaugas Greibus
 */
class LiepaSphinxRecognitionActivity : AppCompatActivity() {


    private var sphinxRecognizer: SpeechRecognizer? = null
    private val viewModelJob = Job()
    private val uiScope = CoroutineScope(Dispatchers.Main + viewModelJob)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_atpazintuvas_main)
        //request keep awake as we not touching screen
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "[onResume]")
        if (checkPermissionForRecognition()) {
            Log.d(TAG, "[onResume]Granted all permission")
            uiScope.launch {
                Log.d(TAG, "[uiScope.async]Initialize")
                val assets = Assets(baseContext)
                val assetDir = assets.syncAssets()
                sphinxRecognizer = setupRecognizer(assetDir)
                longToast(resources.getString(R.string.msg_liepa_started))
                restartRecording(sphinxRecognizer)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        shutdownRecognition()
    }

    override fun onPause() {
        super.onPause()
        shutdownRecognition()
    }

    /**
     * Check if already user granted recording and external file storage permissions.
     */
    private fun checkPermissionForRecognition(): Boolean {
        Log.d(TAG, "[checkPermissionForRecognition]+++")

        val permissionAudioCheck =
            ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.RECORD_AUDIO)
        if (permissionAudioCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                PERMISSIONS_REQUEST_RECORD_AUDIO
            )
            return false
        }
        return true
    }

    /**
     * Initialise recognizer and set acoustic model, dictionary and grammar(as language model)
     */
    private fun setupRecognizer(assetsDir: File): SpeechRecognizer? {
        Log.d(TAG, "[setupRecognizer]")
        val aRecognizer = SpeechRecognizerSetup.defaultSetup()
            .setAcousticModel(File(assetsDir, ACOUSTIC_MODEL_PATH))
            .setDictionary(File(assetsDir, DICTIONARY_FILE))
            .recognizer

        val recognitionListenerImpl = object : RecognitionListener {
            /**
             * Called by recognizer when final result is found: after recognizer.stop() or timeout.
             */
            override fun onResult(hypothesis: Hypothesis?) {
                if (hypothesis == null || hypothesis.hypstr == null || hypothesis.hypstr.isEmpty()) {
                    Log.d(TAG, "[onResult] hypstr:????")
                    restartRecording(sphinxRecognizer)
                    return
                }
                val hypstr = hypothesis.hypstr
                Log.d(TAG, "[onResult] hypstr:$hypstr")
                resultTextView.text = "Atpažinta: " + hypstr
                restartRecording(sphinxRecognizer)
            }
            /**
             * Called by recognizer when partial result is found, but audio recording continues.
             */
            override fun onPartialResult(hypothesis: Hypothesis?) {
                if (hypothesis == null || hypothesis.hypstr == null || hypothesis.hypstr.isEmpty()) {
                    //Log.d(TAG, "[onPartialResult] hypstr:????????")
                    return
                }
                val hypstr = hypothesis.hypstr
                Log.d(TAG, "[onPartialResult] hypstr:$hypstr")
                resultTextView.text = "Sakoma: " + hypstr + " ..."
            }

            /**
             * Called by recognizer when silence detected then set timeout time
             */
            override fun onTimeout() {
                Log.d(TAG, "[onTimeout]")
                sphinxRecognizer?.stop()
            }
            /**
             * Called by recognizer when bengining for speech segment was found in audio signal.
             */
            override fun onBeginningOfSpeech() {
                Log.d(TAG, "[onBeginningOfSpeech]")
            }
            /**
             * Called by recognizer when end of speech was found in audio signal.
             */
            override fun onEndOfSpeech() {
                Log.d(TAG, "[onEndOfSpeech]")
            }
            /**
             * Called by recognizer when something bad happens.
             */
            override fun onError(error: Exception?) {
                Log.e(TAG, "[onError] " + error?.message, error)
                resultTextView.text = error?.message
            }

        }

        aRecognizer?.let {
            it.addListener(recognitionListenerImpl)
            val adrPhraseGrammar = File(assetsDir, "models/FZ1.3/language/adr_frazės.gram")
            it.addGrammarSearch(GRAMMAR, adrPhraseGrammar)
        }
        return aRecognizer
    }

    /**
     * Stops and start recognizer when app is starting or recognizer recognized phrase (onResult).
     */
    private fun restartRecording(recognizer: SpeechRecognizer?) {
        Log.d(TAG, "[restartRecording]")
        if (recognizer == null) return
        //after this stop RecognitionListener#onResult will be invoked
        recognizer.stop()
        uiScope.launch {
            //give 3 seconds to read previous message
            delay(3000)
            //remind what can be said
            resultTextView.text = resources.getString(R.string.msg_phrases_for_recognition)
            //start listening again
            recognizer.startListening(GRAMMAR, 2000)//start listening with timeout 2 seconds.
        }


    }

    /**
     * Turning off recognition and other resources.
     */
    private fun shutdownRecognition() {
        Log.i(TAG, "[shutdownRecognition]")
        sphinxRecognizer?.let {
            it.cancel()
            it.shutdown()
        }
        longToast(resources.getString(R.string.msg_liepa_stoped))
    }

}
