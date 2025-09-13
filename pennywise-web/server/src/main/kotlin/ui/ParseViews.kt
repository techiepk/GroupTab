package com.example.ui

import com.pennywiseai.parser.core.ParsedTransaction
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.html.*
import io.ktor.server.response.*
import kotlinx.html.*

object ParseViews {
    suspend fun ApplicationCall.respondParsePage() {
        respondHtml(HttpStatusCode.OK) {
            head {
              title { +"PennyWise AI" }
                meta { charset = "utf-8" }
                meta { name = "viewport"; content = "width=device-width, initial-scale=1" }
                script { src = "https://unpkg.com/htmx.org@1.9.12" }
                style {
                    unsafe {
                        +"""
                        :root { color-scheme: dark; }
                        * { box-sizing: border-box; }
                        body { background: #000; color: #fff; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, 'Fira Sans', 'Droid Sans', 'Helvetica Neue', Arial, sans-serif; margin: 0; line-height: 1.5; }
                        .container { max-width: 860px; margin: 0 auto; padding: 24px; }
                        h1 { margin: 0 0 6px 0; font-size: 22px; }
                        label { display: block; font-weight: 600; margin: 12px 0 6px; color: #fff; }
                        input[type=text], textarea { width: 100%; padding: 12px 12px; background: #0a0a0a; border: 1px solid #333; border-radius: 8px; font-size: 14px; color: #fff; }
                        input[type=text]::placeholder, textarea::placeholder { color: #9ca3af; }
                        input[type=text]:focus, textarea:focus { outline: none; border-color: #555; box-shadow: 0 0 0 3px rgba(255,255,255,0.06); }
                        textarea { min-height: 160px; resize: vertical; }
                        button { margin-top: 12px; padding: 10px 16px; background: #fff; color: #000; border: 1px solid #fff; border-radius: 8px; cursor: pointer; font-weight: 600; }
                        button:hover { background: #000; color: #fff; }
                        button:disabled { opacity: .6; cursor: not-allowed; }
                        .card { background: #0a0a0a; border: 1px solid #222; border-radius: 10px; padding: 16px; margin-top: 16px; }
                        .muted { color: #9ca3af; font-size: 12px; }
                        .row { display: grid; grid-template-columns: 160px 1fr; gap: 8px; margin: 6px 0; }
                        .row > div:first-child { color: #9ca3af; }
                        .spinner { display:none; width:16px; height:16px; border:2px solid #333; border-top-color:#fff; border-radius:50%; animation: spin 1s linear infinite; margin-left: 8px; vertical-align: middle; }
                        .htmx-request .spinner { display:inline-block; }
                        @keyframes spin { to { transform: rotate(360deg) } }
                        @media (max-width: 640px) {
                          .container { padding: 16px; }
                          .row { grid-template-columns: 1fr; }
                          h1 { font-size: 18px; }
                        }
                        """
                    }
                }
            }
            body {
                div(classes = "container") {
                    h1 { +"PennyWise AI" }
                    p(classes = "muted") { +"Quickly test parsing by sender ID and message body." }

                    form {
                        attributes["hx-post"] = "/htmx/parse"
                        attributes["hx-target"] = "#result"
                        attributes["hx-swap"] = "innerHTML"
                        attributes["hx-indicator"] = "#indicator"

                        label { htmlFor = "sender"; +"Sender ID" }
                        input(type = InputType.text, name = "sender") {
                            id = "sender"; placeholder = "e.g., VM-HDFC, AD-SBIBK-S"; required = true
                        }

                        label { htmlFor = "smsBody"; +"Message Body" }
                        textArea {
                            id = "smsBody"; name = "smsBody"; placeholder = "Paste SMS body here"; required = true
                        }

                        input(type = InputType.hidden, name = "timestamp") { id = "timestamp" }

                        button(type = ButtonType.submit) { +"Parse" }
                        span("spinner") { id = "indicator" }
                    }

                    div(classes = "card") { id = "result"; p { +"Result will appear here." } }

                    script {
                        unsafe { +"""
                            (function(){
                              var ts = document.getElementById('timestamp');
                              function setTs(){ ts && (ts.value = Date.now()); }
                              setTs();
                              document.addEventListener('htmx:configRequest', function(){ setTs(); });
                            })();
                        """ }
                    }
                }
            }
        }
    }

    fun FlowContent.renderParseResult(parsed: ParsedTransaction?) {
        if (parsed == null) {
            div { p { +"No transaction detected." } }
            return
        }
        div {
            h3 { +"Parsed Transaction" }
            div(classes = "row") { div { +"Bank" }; div { +parsed.bankName } }
            div(classes = "row") { div { +"Type" }; div { +parsed.type.name } }
            div(classes = "row") { div { +"Amount" }; div { +parsed.amount.toPlainString() } }
            if (parsed.merchant != null) {
                div(classes = "row") { div { +"Merchant" }; div { +parsed.merchant!! } }
            }
            if (parsed.reference != null) {
                div(classes = "row") { div { +"Reference" }; div { +parsed.reference!! } }
            }
            if (parsed.accountLast4 != null) {
                div(classes = "row") { div { +"Account Last 4" }; div { +parsed.accountLast4!! } }
            }
            if (parsed.balance != null) {
                div(classes = "row") { div { +"Balance" }; div { +parsed.balance!!.toPlainString() } }
            }
            if (parsed.creditLimit != null) {
                div(classes = "row") { div { +"Available Limit" }; div { +parsed.creditLimit!!.toPlainString() } }
            }
            div(classes = "row") { div { +"From Card" }; div { +(if (parsed.isFromCard) "Yes" else "No") } }
            details {
                summary { +"Raw SMS" }
                pre { +parsed.smsBody }
            }
        }
    }
}


