package com.pennywiseai.tracker.ui.util

import android.content.Context
import android.webkit.WebView
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.html.HtmlGenerator
import org.intellij.markdown.parser.MarkdownParser

class MarkdownRenderer(private val context: Context) {
    
    private val flavour = GFMFlavourDescriptor()
    private val parser = MarkdownParser(flavour)
    
    fun renderToWebView(webView: WebView, markdown: String) {
        val tree = parser.buildMarkdownTreeFromString(markdown)
        val html = HtmlGenerator(markdown, tree, flavour).generateHtml()
        
        val styledHtml = wrapWithStyles(html)
        
        webView.apply {
            settings.javaScriptEnabled = false
            settings.domStorageEnabled = false
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            settings.allowUniversalAccessFromFileURLs = false
            settings.allowFileAccessFromFileURLs = false
            settings.setGeolocationEnabled(false)
            settings.setSupportZoom(false)
            settings.builtInZoomControls = false
            settings.displayZoomControls = false
            setBackgroundColor(0) // Transparent background
            loadDataWithBaseURL(null, styledHtml, "text/html", "UTF-8", null)
        }
    }
    
    private fun wrapWithStyles(html: String): String {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    body {
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                        font-size: 16px;
                        line-height: 1.5;
                        color: #333;
                        background-color: transparent;
                        margin: 0;
                        padding: 8px;
                    }
                    
                    h1, h2, h3, h4, h5, h6 {
                        color: #2c3e50;
                        margin: 12px 0 8px 0;
                        font-weight: 600;
                    }
                    
                    h1 { font-size: 1.5em; }
                    h2 { font-size: 1.3em; }
                    h3 { font-size: 1.1em; }
                    
                    p {
                        margin: 8px 0;
                    }
                    
                    ul, ol {
                        margin: 8px 0;
                        padding-left: 20px;
                    }
                    
                    li {
                        margin: 4px 0;
                    }
                    
                    strong {
                        font-weight: 600;
                        color: #2c3e50;
                    }
                    
                    em {
                        font-style: italic;
                        color: #555;
                    }
                    
                    code {
                        background-color: #f1f2f6;
                        padding: 2px 4px;
                        border-radius: 3px;
                        font-family: 'Courier New', monospace;
                        font-size: 0.9em;
                        color: #e74c3c;
                    }
                    
                    pre {
                        background-color: #f8f9fa;
                        padding: 12px;
                        border-radius: 6px;
                        overflow-x: auto;
                        margin: 8px 0;
                    }
                    
                    pre code {
                        background-color: transparent;
                        padding: 0;
                        color: #333;
                    }
                    
                    blockquote {
                        border-left: 4px solid #3498db;
                        padding-left: 16px;
                        margin: 8px 0;
                        color: #666;
                        font-style: italic;
                    }
                    
                    table {
                        border-collapse: collapse;
                        width: 100%;
                        margin: 8px 0;
                    }
                    
                    th, td {
                        border: 1px solid #ddd;
                        padding: 8px;
                        text-align: left;
                    }
                    
                    th {
                        background-color: #f8f9fa;
                        font-weight: 600;
                    }
                    
                    a {
                        color: #3498db;
                        text-decoration: none;
                    }
                    
                    a:hover {
                        text-decoration: underline;
                    }
                    
                    hr {
                        border: none;
                        border-top: 1px solid #ddd;
                        margin: 16px 0;
                    }
                    
                    /* Dark theme support */
                    @media (prefers-color-scheme: dark) {
                        body {
                            color: #e1e1e1;
                        }
                        
                        h1, h2, h3, h4, h5, h6 {
                            color: #ffffff;
                        }
                        
                        strong {
                            color: #ffffff;
                        }
                        
                        em {
                            color: #cccccc;
                        }
                        
                        code {
                            background-color: #2d3748;
                            color: #ffd700;
                        }
                        
                        pre {
                            background-color: #2d3748;
                        }
                        
                        pre code {
                            color: #e1e1e1;
                        }
                        
                        blockquote {
                            color: #cccccc;
                        }
                        
                        th {
                            background-color: #2d3748;
                        }
                        
                        th, td {
                            border-color: #4a5568;
                        }
                        
                        hr {
                            border-color: #4a5568;
                        }
                    }
                </style>
            </head>
            <body>
                $html
            </body>
            </html>
        """.trimIndent()
    }
}