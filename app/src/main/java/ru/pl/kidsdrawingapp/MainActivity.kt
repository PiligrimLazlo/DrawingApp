package ru.pl.kidsdrawingapp

import android.Manifest
import android.app.AlertDialog
import android.app.Dialog
import android.graphics.Color
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.get
import top.defaults.colorpicker.ColorPickerPopup

class MainActivity : AppCompatActivity() {

    private var drawingView: DrawingView? = null
    private var mImageButtonCurrentPaint: ImageButton? = null

    //для колеса выбора
    private var tvPickColor: TextView? = null

    //выбор картинки
    private var btnGallery: ImageButton? = null
    private var galleryResultLauncher: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            permissions.entries.forEach {
                val permissionName = it.key
                val isGranted = it.value
                if (isGranted)
                    Toast.makeText(
                        this, "Разрешение для галлереи выдано",
                        Toast.LENGTH_SHORT
                    ).show()
                else {
                    if (permissionName.equals(Manifest.permission.READ_EXTERNAL_STORAGE))
                        Toast.makeText(
                            this, "Разрешение для галлереи не выдано",
                            Toast.LENGTH_SHORT
                        ).show()
                }
            }
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnGallery = findViewById(R.id.ibGallery)
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
            pickUpColorFromWheel(view)
            highlightPickedColorButton(view)
        }

        btnGallery?.setOnClickListener {
            selectStoragePermission();
        }

    }


    private fun selectStoragePermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this, Manifest.permission.READ_EXTERNAL_STORAGE
            )
        ) {
            showRationaleDialog(
                "Разрешение на использование галереи не получено!",
                "Ошибка доступа к галерее"
            )
        } else {
            galleryResultLauncher.launch(arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE
                //TODO добавить writing external storage permission
                ))
        }
    }

    private fun showRationaleDialog(title: String, message: String) {
        val builder = AlertDialog.Builder(this)
        builder.setMessage(message)
            .setTitle(title)
            .setCancelable(false)
            .setPositiveButton("ОК") { dialog, _ ->
                dialog.dismiss()
            }
            .create().show()
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