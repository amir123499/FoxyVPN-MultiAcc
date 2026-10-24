package com.vauth.foxyvpn.ui.i18n

import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit

/** Languages the UI strings are available in. */
enum class UiLang { En, Fa }

/** The language the UI strings are rendered in. Provided once, at the app root. */
val LocalUiLang = staticCompositionLocalOf { UiLang.En }

/**
 * Looks up [text] for [lang]. English (and anything without a translation)
 * is returned unchanged, so user data such as emails or server names is
 * never altered.
 */
fun translate(text: String, lang: UiLang): String {
    if (lang == UiLang.En || text.isEmpty()) return text
    PersianStrings.exact[text]?.let { return it }
    // Runtime-assembled strings are short; long text (log lines etc.) is skipped.
    if (text.length > 120 || text.indexOf('\n') >= 0) return text
    for ((regex, template) in PersianStrings.rules) {
        val match = regex.matchEntire(text) ?: continue
        return expand(template, match)
    }
    return text
}

private fun expand(template: String, match: MatchResult): String {
    val out = StringBuilder(template.length + 16)
    var i = 0
    while (i < template.length) {
        val c = template[i]
        if (c == '{' && i + 2 < template.length && template[i + 1].isDigit() && template[i + 2] == '}') {
            val group = template[i + 1] - '0'
            out.append(match.groupValues.getOrElse(group) { "" })
            i += 3
        } else {
            out.append(c)
            i++
        }
    }
    return out.toString()
}

/**
 * Drop-in replacement for material3's Text(String). Screens import this
 * instead, so every label is translated when drawn.
 */
@Composable
fun Text(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    style: TextStyle = LocalTextStyle.current,
) {
    androidx.compose.material3.Text(
        text = translate(text, LocalUiLang.current),
        modifier = modifier,
        color = color,
        fontSize = fontSize,
        fontStyle = fontStyle,
        fontWeight = fontWeight,
        fontFamily = fontFamily,
        letterSpacing = letterSpacing,
        textDecoration = textDecoration,
        textAlign = textAlign,
        lineHeight = lineHeight,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        minLines = minLines,
        onTextLayout = onTextLayout,
        style = style,
    )
}

/** Same as [Text] for styled text. Text that carries spans is left untouched. */
@Composable
fun Text(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    inlineContent: Map<String, InlineTextContent> = mapOf(),
    onTextLayout: (TextLayoutResult) -> Unit = {},
    style: TextStyle = LocalTextStyle.current,
) {
    val lang = LocalUiLang.current
    val shown = if (text.spanStyles.isEmpty() && text.paragraphStyles.isEmpty()) {
        val t = translate(text.text, lang)
        if (t == text.text) text else AnnotatedString(t)
    } else {
        text
    }
    androidx.compose.material3.Text(
        text = shown,
        modifier = modifier,
        color = color,
        fontSize = fontSize,
        fontStyle = fontStyle,
        fontWeight = fontWeight,
        fontFamily = fontFamily,
        letterSpacing = letterSpacing,
        textDecoration = textDecoration,
        textAlign = textAlign,
        lineHeight = lineHeight,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        minLines = minLines,
        inlineContent = inlineContent,
        onTextLayout = onTextLayout,
        style = style,
    )
}
