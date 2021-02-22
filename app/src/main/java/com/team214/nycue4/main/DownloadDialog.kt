package com.team214.nycue4.main


import android.content.DialogInterface
import android.content.Intent
import android.content.res.Resources
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.MimeTypeMap
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.team214.nycue4.R
import kotlinx.android.synthetic.main.dialog_download.*
import java.io.File


class DownloadDialog : BottomSheetDialogFragment() {

    private var onDismissListener: DialogInterface.OnDismissListener? = null

    fun setOnDismissListener(onDismissListener: DialogInterface.OnDismissListener) {
        this.onDismissListener = onDismissListener
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        if (onDismissListener != null) {
            onDismissListener!!.onDismiss(dialog)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_download, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val file = arguments?.getSerializable("file") as File
        download_delete?.setOnClickListener {
            val deleteDialog = AlertDialog.Builder(context!!)
            deleteDialog.setTitle(getString(R.string.delete))
                .setMessage(getString(R.string.confirm_delete, file.name))
                .setPositiveButton(R.string.positive) { _, _ ->
                    file.delete()
                    dismissAllowingStateLoss()
                }
                .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.cancel() }
            deleteDialog.show()
        }
        download_rename?.setOnClickListener {
            val editDialogBuild = AlertDialog.Builder(context!!)
            editDialogBuild.setTitle(R.string.rename)
            val editText = EditText(context)
            editText.setText(file.name)
            editText.maxLines = 1
            editText.inputType = InputType.TYPE_CLASS_TEXT
            editDialogBuild.setView(editText)
            editText.requestFocus()
            editText.setSelection(0, file.nameWithoutExtension.length)
            editDialogBuild.setPositiveButton(R.string.positive) { dialog, _ ->
                file.renameTo(File(file.parentFile, editText.text.toString()))
                dialog.cancel()
                dismissAllowingStateLoss()
            }.setNegativeButton(R.string.cancel) { _, _ -> dismissAllowingStateLoss() }
            val spacing = (20 * Resources.getSystem().displayMetrics.density).toInt()
            editDialogBuild.setView(editText, spacing, 0, spacing, 0)
            val editDialog = editDialogBuild.create()
            editDialog.window!!.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
            editDialog.show()
        }
        download_share?.setOnClickListener {
            val intent = Intent(Intent.ACTION_SEND)
            val type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.extension)
            val fileUri = FileProvider.getUriForFile(
                context!!,
                context!!.applicationContext.packageName + ".provider", file
            )
            intent.setDataAndType(fileUri, type)
            intent.putExtra(Intent.EXTRA_STREAM, fileUri)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            context!!.startActivity(intent)
            dismissAllowingStateLoss()
        }
    }

}