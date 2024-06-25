package com.example.kidsdrawing

import android.Manifest
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.media.MediaScannerConnection
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

import androidx.core.view.get
import androidx.lifecycle.lifecycleScope
import com.example.kidsdrawing.databinding.ActivityMainBinding
import com.example.kidsdrawing.databinding.DialogBrushSizeBinding
import com.example.kidsdrawing.databinding.DialogCustomProgressBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var dialogBinding:DialogBrushSizeBinding
    private lateinit var progressBinding: DialogCustomProgressBinding
    private var mImageButtonCurrentPaint:ImageButton?=null
    var customProgressDialog:Dialog?=null


    private val openGalleryLauncher:ActivityResultLauncher<Intent> =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
                result ->
                if(result.resultCode== RESULT_OK && result.data!=null){
                    binding.ivBackground.setImageURI(result.data?.data)
                }
            }


    private val requestPermission: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            permissions ->
            permissions.entries.forEach {
                val permissionName = it.key
                val isGranted = it.value
                if (isGranted ) {
                    Toast.makeText(
                        this@MainActivity,
                        "Permission granted now you can read the storage files.",
                        Toast.LENGTH_LONG
                    ).show()
                    val pickIntent=Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                    openGalleryLauncher.launch(pickIntent)
                }

                else {

                    if (permissionName == Manifest.permission.READ_MEDIA_IMAGES)
                        Toast.makeText(
                            this@MainActivity,
                            "Oops you just denied the permission.",
                            Toast.LENGTH_LONG
                        ).show()
                }
            }

        }




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding=ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
//        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
//            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
//            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
//            insets
//        }
        binding.drawingView.setSizeForBrush(20.toFloat())

        val linearLayoutPaintColors=binding.llPaintColors
        
        mImageButtonCurrentPaint=linearLayoutPaintColors[1] as ImageButton
        mImageButtonCurrentPaint!!.setImageDrawable(
            ContextCompat.getDrawable(this,R.drawable.pallet_pressed)
        )

        binding.ibBrush.setOnClickListener{
            showBrushSizeChooserDialog()
        }
        binding.ibGallery.setOnClickListener {
            requestStoragePermission()
        }
        binding.ibUndo.setOnClickListener {
            binding.drawingView.onClickUndo()

        }
        binding.ibRedo.setOnClickListener {
            binding.drawingView.OnClickRedo()
        }
        binding.ibSave.setOnClickListener {
            if (isReadStorageAllowed()){
                showProgressDialog()
                //launch a coroutine block
                lifecycleScope.launch{
                    //reference the frame layout
                    val flDrawingView: FrameLayout =binding.flDrawingViewContainer
                    //Save the image to the device
                    saveBitmapFile(getBitmapFromView(flDrawingView))
                }
            }
        }

    }
    private fun showBrushSizeChooserDialog(){
        val brushDialog=Dialog(this)
        dialogBinding = DialogBrushSizeBinding.inflate(layoutInflater)
        brushDialog.setContentView(dialogBinding.root)
        brushDialog.setTitle("Brush Size:")
        val smallBtn:ImageButton=dialogBinding.ibSmallBrush
        smallBtn.setOnClickListener(View.OnClickListener {
            binding.drawingView.setSizeForBrush(10.toFloat())
            brushDialog.dismiss()
            })
        val mediumBtn:ImageButton=dialogBinding.ibMediumBrush
        mediumBtn.setOnClickListener(View.OnClickListener {
            binding.drawingView.setSizeForBrush(20.toFloat())
            brushDialog.dismiss()
        })
        val largeBtn:ImageButton=dialogBinding.ibLargeBrush
        largeBtn.setOnClickListener(View.OnClickListener {
            binding.drawingView.setSizeForBrush(30.toFloat())
            brushDialog.dismiss()
        })
        brushDialog.show()

    }
    fun paintClicked(view: View){
        if (view !== mImageButtonCurrentPaint) {
            // Update the color
            val imageButton = view as ImageButton

            val colorTag = imageButton.tag.toString()
            // The color is set as per the selected tag here.
            binding.drawingView.setColor(colorTag)
            // Swap the backgrounds for last active and currently active image button.
            imageButton.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.pallet_pressed))
            mImageButtonCurrentPaint?.setImageDrawable(
                ContextCompat.getDrawable(this, R.drawable.pallet_normal)
            )

            //Current view is updated with selected view in the form of ImageButton.
            mImageButtonCurrentPaint = view
        }
    }
    private fun isReadStorageAllowed():Boolean{
        val result=ContextCompat.checkSelfPermission(
            this,Manifest.permission.READ_MEDIA_IMAGES)
        return result== PackageManager.PERMISSION_GRANTED
    }
    private fun requestStoragePermission(){
        if(ActivityCompat.shouldShowRequestPermissionRationale(
            this,Manifest.permission.READ_MEDIA_IMAGES)
            ){
            showRationaleDialog("Kids Drawing App","Kids Drawing App"+"needs to Access your External Storage")
        }else{
            requestPermission.launch(arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ))
        }

    }
    private fun showRationaleDialog(title:String,message:String){
        val builder:AlertDialog.Builder=AlertDialog.Builder(this)
        builder.setTitle(title)
            .setMessage(message)
            .setPositiveButton("Cancel"){dialog,_ ->
                dialog.dismiss()
            }
        builder.create().show()


    }
    private fun getBitmapFromView(view: View): Bitmap {
        //Define a bitmap with the same size as the view.
        // CreateBitmap : Returns a mutable bitmap with the specified width and height
        val returnedBitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        //Bind a canvas to it
        val canvas = Canvas(returnedBitmap)
        //Get the view's background
        val bgDrawable = view.background
        if (bgDrawable != null) {
            //has background drawable, then draw it on the canvas
            bgDrawable.draw(canvas)
        } else {
            //does not have background drawable, then draw white background on the canvas
            canvas.drawColor(Color.WHITE)
        }
        // draw the view on the canvas
        view.draw(canvas)

        return returnedBitmap
    }
    private suspend fun saveBitmapFile(mBitmap:Bitmap):String{
        var result=""
        withContext(Dispatchers.IO){
            if(mBitmap!=null){
                try {
                    val bytes=ByteArrayOutputStream()
                    mBitmap.compress(Bitmap.CompressFormat.PNG, 90, bytes)
                    val f = File(
                        externalCacheDir?.absoluteFile.toString()
                                + File.separator + "KidDrawingApp_" + System.currentTimeMillis() / 1000 + ".jpg"
                    )
                    val fo =
                        FileOutputStream(f) // Creates a file output stream to write to the file represented by the specified object.
                    fo.write(bytes.toByteArray()) // Writes bytes from the specified byte array to this file output stream.
                    fo.close() // Closes this file output stream and releases any system resources associated with this stream. This file output stream may no longer be used for writing bytes.
                    result = f.absolutePath

                    runOnUiThread {
                        cancelProgressDialog()
                        if (!result.isEmpty()) {
                            Toast.makeText(
                                this@MainActivity,
                                "File saved successfully :$result",
                                Toast.LENGTH_SHORT
                            ).show()
                            shareImage(result)
                        } else {
                            Toast.makeText(
                                this@MainActivity,
                                "Something went wrong while saving the file.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }



                catch (e: Exception) {
                    result = ""
                    e.printStackTrace()
                }
            }
        }
        return result

    }
    private fun showProgressDialog(){
        customProgressDialog=Dialog(this)
        progressBinding=DialogCustomProgressBinding.inflate(layoutInflater)
        customProgressDialog?.setContentView(progressBinding.root)
        customProgressDialog?.show()


    }
    private fun cancelProgressDialog() {
        if (customProgressDialog != null) {
            customProgressDialog?.dismiss()
            customProgressDialog = null
        }
    }
    private fun shareImage(result:String){

        /*MediaScannerConnection provides a way for applications to pass a
        newly created or downloaded media file to the media scanner service.
        The media scanner service will read metadata from the file and add
        the file to the media content provider.
        The MediaScannerConnectionClient provides an interface for the
        media scanner service to return the Uri for a newly scanned file
        to the client of the MediaScannerConnection class.*/

        /*scanFile is used to scan the file when the connection is established with MediaScanner.*/
        MediaScannerConnection.scanFile(
            this@MainActivity, arrayOf(result), null
        ) { path, uri ->
            // This is used for sharing the image after it has being stored in the storage.
            val shareIntent = Intent()
            shareIntent.action = Intent.ACTION_SEND
            shareIntent.putExtra(
                Intent.EXTRA_STREAM,
                uri
            ) // A content: URI holding a stream of data associated with the Intent, used to supply the data being sent.
            shareIntent.type =
                "image/png" // The MIME type of the data being handled by this intent.
            startActivity(
                Intent.createChooser(
                    shareIntent,
                    "Share"
                )
            )// Activity Action: Display an activity chooser,
            // allowing the user to pick what they want to before proceeding.
            // This can be used as an alternative to the standard activity picker
            // that is displayed by the system when you try to start an activity with multiple possible matches,
            // with these differences in behavior:
        }
    }
}