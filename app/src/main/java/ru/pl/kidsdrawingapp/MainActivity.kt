package ru.pl.kidsdrawingapp

import android.Manifest
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.media.MediaScannerConnection
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.get
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.*
import top.defaults.colorpicker.ColorPickerPopup
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.lang.Exception

class MainActivity : AppCompatActivity() {

    private var drawingView: DrawingView? = null
    private var mImageButtonCurrentPaint: ImageButton? = null
    var customProgressDialog: Dialog? = null

    //получаем результат - картинку и устанавливаем на фон ivBackground
    private val openGalleryLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK && result.data != null) {
                val imageBackground: ImageView = findViewById(R.id.ivBackground)
                imageBackground.setImageURI(result.data?.data)
            }
        }

    //для колеса выбора
    private var tvPickColor: TextView? = null

    //выбор картинки
    private var btnGallery: ImageButton? = null

    //нужно для получения разрешений(1)
    private var requestPermissionLauncher: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            permissions.entries.forEach {
                val permissionName = it.key
                val isGranted = it.value
                if (isGranted) {
                    Toast.makeText(
                        this, "Разрешение для галлереи выдано",
                        Toast.LENGTH_SHORT
                    ).show()

                    val pickIntent =
                        Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                    openGalleryLauncher.launch(pickIntent)

                } else {
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

        val btnBrush: ImageButton? = findViewById(R.id.ibBrush)
        btnBrush?.setOnClickListener {
            showBrushSizeChooserDialog()
        }

        val btnUndo: ImageButton? = findViewById(R.id.ibUndo)
        btnUndo?.setOnClickListener {
            drawingView?.onClickUndo()
        }

        val btnRedo: ImageButton? = findViewById(R.id.ibRedo)
        btnRedo?.setOnClickListener {
            drawingView?.onClickRedo()
        }

        val btnSave: ImageButton? = findViewById(R.id.ibSave)
        btnSave?.setOnClickListener {
            if (isReadStorageAllowed()) {
                showProgressDialog()
                lifecycleScope.launch {
                    val flDrawingView: FrameLayout = findViewById(R.id.flDrawingViewContainer)
                    saveBitmapFile(getBitmapFromView(flDrawingView))
                }
            }
        }

        tvPickColor?.setOnClickListener { view ->
            pickUpColorFromWheel(view)
            highlightPickedColorButton(view)
        }

        //нужно для получения разрешений(2)
        btnGallery?.setOnClickListener {
            requestStoragePermission();
        }

    }


    private fun isReadStorageAllowed(): Boolean {
        val result =
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.READ_EXTERNAL_STORAGE
            )
        return result == PackageManager.PERMISSION_GRANTED
    }

    private fun requestStoragePermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this, Manifest.permission.READ_EXTERNAL_STORAGE
            )
        ) {
            showRationaleDialog(
                "Разрешение на использование галереи не получено!",
                "Ошибка доступа к галерее"
            )
        } else {
            requestPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            )
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

    private fun getBitmapFromView(view: View): Bitmap {
        val returnedBitmap =
            Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(returnedBitmap)
        val bgDrawable = view.background

        if (bgDrawable != null)
            bgDrawable.draw(canvas)
        else
            canvas.drawColor(Color.WHITE)

        view.draw(canvas)

        return returnedBitmap
    }

    private suspend fun saveBitmapFile(mBitmap: Bitmap?): String {
        var result = ""
        withContext(Dispatchers.IO) {
            if (mBitmap != null) {
                try {
                    val bytes = ByteArrayOutputStream()
                    mBitmap.compress(Bitmap.CompressFormat.PNG, 90, bytes)

                    val f = File(
                        externalCacheDir?.absoluteFile.toString()
                                + File.separator + "KidsDrawingAPP_"
                                + System.currentTimeMillis() / 1000 + ".png"
                    )

                    val fo = FileOutputStream(f)
                    fo.write(bytes.toByteArray())
                    fo.close()

                    result = f.absolutePath

                    runOnUiThread {
                        cancelProgressDialog()
                        if (result.isNotEmpty()) {
                            Toast.makeText(
                                applicationContext,
                                "Файл сохранен в $result",
                                Toast.LENGTH_SHORT
                            ).show()
                            shareImage(result)
                        } else {
                            Toast.makeText(
                                applicationContext,
                                "Файл не сохранен",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                } catch (e: Exception) {
                    result = ""
                    e.printStackTrace()
                }
            }
        }
        return result
    }

    fun showProgressDialog() {
        customProgressDialog = Dialog(this)
        customProgressDialog?.setContentView(R.layout.dialog_custom_progress)
        customProgressDialog?.show()
    }

    private fun cancelProgressDialog() {
        if (customProgressDialog != null) {
            customProgressDialog?.dismiss()
            customProgressDialog = null
        }
    }

    private fun shareImage(result: String) {
        MediaScannerConnection.scanFile(this, arrayOf(result), null) { path, uri ->
            val shareIntent = Intent()
            shareIntent.action = Intent.ACTION_SEND
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
            shareIntent.type = "image/png"
            startActivity(Intent.createChooser(shareIntent, "Поделиться"))
        }
    }


}