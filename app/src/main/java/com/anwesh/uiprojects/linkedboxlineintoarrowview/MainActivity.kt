package com.anwesh.uiprojects.linkedboxlineintoarrowview

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.anwesh.uiprojects.boxlineintoarrowview.BoxLineIntoArrowView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        BoxLineIntoArrowView.create(this)
    }
}
