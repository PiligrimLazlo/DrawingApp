package ru.pl.kidsdrawingapp

import android.app.Dialog
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.get
import top.defaults.colorpicker.ColorPickerPopup

class MainActivity : AppCompatActivity() {

    private var drawingView: DrawingView? = null
    private var mImageButtonCurrentPaint: ImageButton? = null

    //для колеса выбора
    private var tvPickColor: TextView? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvPickColor = findViewById(R.id.tvPickColor)
        drawingView = findViewById(R.id.drawingView)
        drawingView?.setSizeForBrush(20f)
        val linearLayoutPaintColors = findViewById<LinearLayout>(R.id.llPaintColors)

        mImageButtonCurrentPaint = linearLayoutPaintColors[1] as ImageButton
        mImageButtonCurrentPaint!!.setImageDrawable(
            ContextCompat.getDrawable(this, R.drawable.pallet_pressed)
        )

        var btnBrush: ImageButton? = findViewById(R.id.ibBrush)
        btnBrush?.setOnClickListener {
            showBrushSizeChooserDialog()
        }

        tvPickColor?.setOnClickListener { view ->
            run {
                pickUpColorFromWheel(view)

                highlightPickedColorButton(view)
            }
        }

    }

    //выбор размера кисти
    private fun showBrushSizeChooserDialog() {
        val brushDialog = Dialog(this)
        brushDialog.setContentView(R.layout.dialog_brush_size)
        brushDialog.setTitle("Размер кисти: ")
        val smallBtn: ImageButton = brushDialog.findViewById(R.id.ibSmallBrush)
        smallBtn.setOnClickListener {
            drawingView?.setSizeForBrush(10f)
            brushDialog.dismiss()
        }
        val mediumBtn: ImageButton = brushDialog.findViewById(R.id.ibMediumBrush)
        mediumBtn.setOnClickListener {
            drawingView?.setSizeForBrush(20f)
            brushDialog.dismiss()
        }
        val largeBtn: ImageButton = brushDialog.findViewById(R.id.ibLargeBrush)
        largeBtn.setOnClickListener {
            drawingView?.setSizeForBrush(30f)
            brushDialog.dismiss()
        }

        brushDialog.show()
    }

    //выбор цвета
    fun paintClicked(view: View) {
        if (view != mImageButtonCurrentPaint) {
            val imageButton = view as ImageButton
            val colorTag = imageButton.tag.toString()
            drawingView?.setColor(colorTag)

            highlightPickedColorButton(view)

        }
    }

    //выделяет кнопку выбранного цвета, снимает выделение с прошлой
    private fun highlightPickedColorButton(clickedView: View) {

        when (clickedView) {
            is TextView -> {
                tvPickColor?.background = ContextCompat.getDrawable(
                    this,
                    R.drawable.pallet_pressed
                )
                mImageButtonCurrentPaint?.setImageDrawable(
                    ContextCompat.getDrawable(this, R.drawable.pallet_normal)
                )
                mImageButtonCurrentPaint = null
            }
            is ImageButton -> {
                clickedView.setImageDrawable(
                    ContextCompat.getDrawable(this, R.drawable.pallet_pressed)
                )

                mImageButtonCurrentPaint?.setImageDrawable(
                    ContextCompat.getDrawable(this, R.drawable.pallet_normal)
                )

                tvPickColor?.background = ContextCompat.getDrawable(
                    this,
                    R.drawable.pallet_normal
                )
                mImageButtonCurrentPaint = clickedView
            }
        }


    }

    //выбор цвета колесом
    private fun pickUpColorFromWheel(view: View) {

        ColorPickerPopup.Builder(this).initialColor(drawingView!!.color)
            .enableBrightness(false)
            .enableAlpha(false)
            .okTitle(" Выбрать")
            .cancelTitle("Отменить")
            .showIndicator(true)
            .showValue(false)
            .build()
            .show(view, object : ColorPickerPopup.ColorPickerObserver() {
                override fun onColorPicked(color: Int) {
                    val r = Color.red(color)
                    val g = Color.green(color)
                    val b = Color.blue(color)
                    val hex = String.format("#%02x%02x%02x", r, g, b)
                    drawingView?.setColor(hex)
                }
            })

    }
}