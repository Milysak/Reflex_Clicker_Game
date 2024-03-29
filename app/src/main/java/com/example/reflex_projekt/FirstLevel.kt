package com.example.reflex_projekt

import android.R.attr.*
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import com.example.reflex_projekt.database.AppDatabase
import com.example.reflex_projekt.database.Stats2x2
import kotlinx.coroutines.*
import kotlin.random.Random


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [FirstLevel.newInstance] factory method to
 * create an instance of this fragment.
 */
class FirstLevel : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    private var refreshNumber = 50
    private var refreshCounter = 0
    private var points = 0
    private var maxTimeInMilis = 10*60*1000L
    private var timeInMilis = 0L

    private lateinit var timer: CountDownTimer

    private var listOfSquares = mutableListOf<View>()

    private var listOfColors = arrayListOf("#167288", "#8cdaec", "#b45248", "#d48c84", "#a89a49", "#d6cfa2",
        "#3cb464", "#9bddb1", "#643c6a", "#836394", "#d25935", "#5d73d7", "#cc3284", "#b88e5d", "#8ababc", "#415e20")
    private var goldColor = "#fbd815"

    private lateinit var pointsField: TextView
    private lateinit var timeField: TextView
    private lateinit var refreshNumberField: TextView
    private lateinit var nameOfLevel: TextView
    private lateinit var recordField: TextView

    private lateinit var btnBack: Button
    private lateinit var btnStart: Button

    private val audio = ToneGenerator(AudioManager.STREAM_MUSIC, 1000)

    private lateinit var appDatabase: AppDatabase

    private var listOfRecords = mutableListOf<Stats2x2>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    @SuppressLint("DiscouragedApi")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_first_level, container, false)

        appDatabase = AppDatabase.getDatabase(requireContext())

        pointsField = view.findViewById(R.id.pointsField)
        timeField = view.findViewById(R.id.timeField)
        refreshNumberField = view.findViewById(R.id.refreshNumber)
        recordField = view.findViewById(R.id.recordField)
        nameOfLevel = view.findViewById(R.id.nameOfLevel)

        btnBack = view.findViewById(R.id.buttonBack)
        btnStart = view.findViewById(R.id.buttonStart)

        dataInit()

        timer = object : CountDownTimer(maxTimeInMilis, 1) {
            override fun onTick(p0: Long) {
                timeInMilis = maxTimeInMilis - p0
                timeField.text =  timeFormatConvert(timeInMilis)
            }

            override fun onFinish() {
                endGame()
            }
        }

        for (i in 0..3) {
            val id = resources.getIdentifier("item_$i", "id", context?.packageName)
            listOfSquares.add(view.findViewById(id) as View)
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        readRecords()

        startAnimation()

        listOfSquares.forEach {
            it.isEnabled = false
        }

        pointsField.isVisible = false
        timeField.isVisible = false
        refreshNumberField.isVisible = false

        btnBack.setOnClickListener {
            Navigation.findNavController(view).navigate(R.id.action_firstLevel_to_mainMenu)
        }

        btnStart.setOnClickListener {
            startGame()
        }

        listOfSquares.forEach {
            it.setOnClickListener {
                if((it.background as ColorDrawable).color == Color.parseColor(goldColor)) {
                    audio.startTone(ToneGenerator.TONE_CDMA_ALERT_INCALL_LITE, 150)
                    points++
                } else {
                    if(points > 0) {
                        audio.startTone(ToneGenerator.TONE_CDMA_PIP, 150)
                        points--
                    }
                }

                refreshArea()

                if(refreshCounter - 1 == refreshNumber) {
                    endGame()
                }
            }
        }

        (activity?.let {
            val builder =
                AlertDialog.Builder(it).setMessage("LEVEL 1 - plansza 2x2\n" +
                        "\nGra polega na klikaniu w ŻÓŁTY kwadrat." +
                        "\nPlansza zmienia swoje ułożenie po kliknięciu w nią.\n" +
                        "\nTrafienie w ŻÓŁTY kwadrat dodaje punkt a kliknięcie w kwadrat" +
                        " o innym kolorze odejmuje go.\n" +
                        "\nGra kończy się po 50 kliknięciach lub po upływie 10 minut.\n" +
                        "\nPowodzenia!")
            builder.apply {
                setPositiveButton("DALEJ",
                    DialogInterface.OnClickListener { dialog, id ->
                        //
                    })
            }
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")).show()
    }

    private fun startAnimation() {
        val animation1 = AnimationUtils.loadAnimation(this.context, R.anim.anim_1)

        recordField.startAnimation(animation1)
    }

    private fun dataInit() {
        runBlocking(Dispatchers.IO) {
           listOfRecords = appDatabase.stats2x2Dao().getAll()
        }
    }

    @SuppressLint("SetTextI18n")
    fun readRecords() {
        val list = mutableListOf<Stats2x2>()

        var maxPointsResult = 0
        var minTimeResult: Long = 10*60*1000

        var recordObject = Stats2x2(50000L, 0)

        if(listOfRecords.isNotEmpty()) {
            listOfRecords.forEach {
                if(it.points2x2 >= maxPointsResult) {
                    maxPointsResult = it.points2x2
                }
            }

            listOfRecords.forEach {
                if(it.points2x2 == maxPointsResult) {
                    list.add(it)
                }
            }

            list.forEach {
                if(it.time2x2 < minTimeResult) {
                    minTimeResult = it.time2x2
                    recordObject = it
                }
            }

            recordField.text = "Twój rekord: \nPunktów: ${recordObject.points2x2}/50\nCzas: ${timeFormatConvert(recordObject.time2x2)}"
        }
        else {
            recordField.text = "Twój rekord: \nBrak rekordów!"
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun writeResultToRecords(timeInMilis: Long, points: Int) {
        GlobalScope.launch(Dispatchers.IO) {
            appDatabase.stats2x2Dao().insert(Stats2x2(timeInMilis, points))
        }

        listOfRecords.add(Stats2x2(timeInMilis, points))
    }

    fun timeFormatConvert(timeInMilis: Long): String {
        var zero = "0"
        if ((timeInMilis/1000)%60 > 9) zero = ""

        var doubleZero = "00"
        if (timeInMilis%1000 > 9) doubleZero = "0"
        if (timeInMilis%1000 > 99) doubleZero = ""

        return "0${(timeInMilis/1000)/60}:$zero${(timeInMilis/1000)%60}:$doubleZero${timeInMilis%1000}"
    }

    fun startGame() {
        refreshArea()

        timer.start()

        listOfSquares.forEach {
            it.isEnabled = true
        }

        btnBack.isVisible = false
        btnStart.isVisible = false
        nameOfLevel.isVisible = false

        pointsField.isVisible = true
        timeField.isVisible = true
        refreshNumberField.isVisible = true
    }

    @SuppressLint("SetTextI18n")
    fun refreshArea() {
        listOfSquares.forEach {
            it.setBackgroundColor(Color.parseColor(listOfColors[Random.nextInt(0, listOfColors.size)]))
        }

        refreshCounter++

        pointsField.text = "Punkty: $points"
        refreshNumberField.text = "$refreshCounter/$refreshNumber"

        listOfSquares[Random.nextInt(0, listOfSquares.size)].setBackgroundColor(Color.parseColor(goldColor))
    }

    fun endGame() {
        listOfSquares.forEach {
            it.setBackgroundColor(Color.WHITE)
            it.isEnabled = false
        }

        timer.cancel()

        writeResultToRecords(timeInMilis, points)

        readRecords()

        btnBack.isVisible = true
        btnStart.isVisible = true
        nameOfLevel.isVisible = true

        pointsField.isVisible = false
        timeField.isVisible = false
        refreshNumberField.isVisible = false

        (activity?.let {
            val builder =
                AlertDialog.Builder(it).setMessage("\nTwój wynik: \nCzas: ${timeFormatConvert(timeInMilis)} \nPunkty: $points/$refreshNumber \n\nGratulacje!")
            builder.apply {
                setPositiveButton("DALEJ",
                    DialogInterface.OnClickListener { dialog, id ->
                        //
                    })
            }
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")).show()

        refreshCounter = 0
        timeInMilis = 0
        points = 0
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment FirstLevel.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            FirstLevel().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}