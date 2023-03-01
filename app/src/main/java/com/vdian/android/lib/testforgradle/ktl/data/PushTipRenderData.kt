package com.vdian.android.lib.testforgradle.ktl.data

/**
 * @author yulun
 * @since 2022-10-31 16:53
 */
class PushTipRenderData(
    var mainTitle: String,
    var subheading: String,
    var buttonText: String,
    var imageUrl: String = ""
) {
    constructor(mainTitle: String, subheading: String, buttonText: String) : this(
        mainTitle,
        subheading,
        buttonText,
        ""
    )
}