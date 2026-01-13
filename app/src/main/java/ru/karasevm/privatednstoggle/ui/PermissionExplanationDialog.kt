package ru.karasevm.privatednstoggle.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import ru.karasevm.privatednstoggle.R

@Composable
fun PermissionExplanationDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val linkColor = MaterialTheme.colorScheme.primary

    val intro = stringResource(R.string.permission_intro)
    val howTitle = stringResource(R.string.permission_how_title)
    val howDetails = stringResource(R.string.permission_how_details)
    val how1 = stringResource(R.string.permission_how_1)
    val how2 = stringResource(R.string.permission_how_2)
    val shizukuText = stringResource(R.string.permission_shizuku_text)
    val shizukuUrl = stringResource(R.string.permission_shizuku_url)
    val manualText = stringResource(R.string.permission_manual_text)
    val manualUrl = stringResource(R.string.permission_manual_url)
    val footer = stringResource(R.string.permission_footer)

    val annotatedText = remember(intro, howTitle, howDetails, how1, how2, shizukuText, manualText, linkColor) {
        buildAnnotatedString {
            append(intro)
            append("\n\n")

            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp)) {
                append(howTitle)
            }
            append("\n$howDetails\n\n")

            append("• ")
            val parts1 = how1.split($$"%1$s")
            if (parts1.size == 2) {
                append(parts1[0])

                withLink(
                    LinkAnnotation.Url(
                        url = shizukuUrl,
                        styles = TextLinkStyles(
                            style = SpanStyle(
                                color = linkColor,
                                textDecoration = TextDecoration.Underline
                            )
                        )
                    )
                ) {
                    append(shizukuText)
                }
                append("${parts1[1]}\n")
            }

            append("• ")
            val parts2 = how2.split($$"%1$s")
            if (parts2.size == 2) {
                append(parts2[0])

                withLink(
                    LinkAnnotation.Url(
                        url = manualUrl,
                        styles = TextLinkStyles(
                            style = SpanStyle(
                                color = linkColor,
                                textDecoration = TextDecoration.Underline
                            )
                        )
                    )
                ) {
                    append(manualText)
                }
                append("${parts2[1]}\n\n")
            }
            append(footer)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.permission_dialog_title),
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = annotatedText,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.ok))
            }
        },
    )
}
