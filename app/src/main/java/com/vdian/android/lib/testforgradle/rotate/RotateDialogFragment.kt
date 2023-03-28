package com.vdian.android.lib.testforgradle.rotate

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import com.vdian.android.lib.testforgradle.R

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [RotateDialogFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class RotateDialogFragment : DialogFragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    val TAG = "RotateDialogFragment"
    override fun onAttachFragment(childFragment: Fragment) {
        super.onAttachFragment(childFragment)
        log("RotateDialogFragment-onAttachFragment")
    }

    override fun onAttach(activity: Activity) {
        super.onAttach(activity)
        log("RotateDialogFragment-onAttach-activity-${activity}")
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        log("RotateDialogFragment-onAttach")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
        log("RotateDialogFragment-onCreate")
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        log("RotateDialogFragment-onCreateView")
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_rotate_dialog, container, false)
    }


    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        log("RotateDialogFragment-onActivityCreated")
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        log("RotateDialogFragment-onSaveInstanceState")
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        log("RotateDialogFragment-onConfigurationChanged")
    }

    override fun onStart() {
        super.onStart()
        log("RotateDialogFragment-onStart")
    }

    override fun onResume() {
        super.onResume()
        log("RotateDialogFragment-onResume")
    }

    override fun onPause() {
        super.onPause()
        log("RotateDialogFragment-onPause")
    }

    override fun onStop() {
        super.onStop()
        log("RotateDialogFragment-onStop")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        log("RotateDialogFragment-onDestroyView")
    }

    override fun onDestroy() {
        super.onDestroy()
        log("RotateDialogFragment-onDestroy")
    }

    override fun onDetach() {
        super.onDetach()
        log("RotateDialogFragment-onDetach")
    }

    private fun log(msg: String) {
        android.util.Log.i(TAG, "$msg")
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment RotateDialogFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            RotateDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}