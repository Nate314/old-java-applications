package launcher;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Window;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.JComponent;

/** Shows an app's README.md in a dialog, with headings, lists, code and bold drawn by Swing's HTML view. */
final class ReadmeView {
   private ReadmeView() {
   }

   static void show(Window owner, String title, Path readme) {
      String md;
      try {
         md = new String(Files.readAllBytes(readme), StandardCharsets.UTF_8);
      } catch (IOException e) {
         md = "Could not read " + readme + ": " + e.getMessage();
      }
      JEditorPane pane = new JEditorPane("text/html", toHtml(md));
      pane.setEditable(false);
      pane.setCaretPosition(0);
      pane.setBorder(BorderFactory.createEmptyBorder(8, 18, 18, 18));
      pane.setBackground(Ui.PANEL);
      JScrollPane scroll = new JScrollPane(pane);
      scroll.setBorder(null);
      Ui.ThinScrollBarUI.apply(scroll.getVerticalScrollBar());
      JDialog d = new JDialog(owner, title + " README", java.awt.Dialog.ModalityType.MODELESS);
      d.getContentPane().add(scroll, BorderLayout.CENTER);
      d.getRootPane().registerKeyboardAction(e -> d.dispose(), KeyStroke.getKeyStroke("ESCAPE"),
         JComponent.WHEN_IN_FOCUSED_WINDOW);
      d.setSize(new Dimension(720, 640));
      d.setLocationRelativeTo(owner);
      d.setVisible(true);
   }

   /** A small Markdown subset: #/##/### headings, - lists, ``` blocks, `code`, **bold**, [links](...) and tables as text. */
   static String toHtml(String md) {
      StringBuilder out = new StringBuilder("<html><body style='font-family:" + Ui.FAMILY
         + ";font-size:11pt;color:#e9ebf1;background:#181b22'>");
      boolean list = false;
      boolean code = false;
      for (String raw : md.replace("\r", "").split("\n", -1)) {
         if (raw.startsWith("```")) {
            if (list) out.append("</ul>");
            list = false;
            out.append(code ? "</pre>" : "<pre style='font-family:Consolas,monospace;font-size:10pt;color:#c9d4ff;"
               + "background:#11131a;padding:8px'>");
            code = !code;
            continue;
         }
         if (code) {
            out.append(escape(raw)).append('\n');
            continue;
         }
         String line = raw.trim();
         boolean item = raw.matches("\\s*[-*] .*");
         if (!item && list) {
            out.append("</ul>");
            list = false;
         }
         if (line.isEmpty()) continue;
         if (item) {
            if (!list) out.append("<ul style='margin-left:18px'>");
            list = true;
            out.append("<li style='margin-bottom:3px'>").append(inline(line.substring(2))).append("</li>");
         } else if (line.startsWith("### ")) {
            out.append("<h3 style='color:#ffffff'>").append(inline(line.substring(4))).append("</h3>");
         } else if (line.startsWith("## ")) {
            out.append("<h2 style='color:#ffffff'>").append(inline(line.substring(3))).append("</h2>");
         } else if (line.startsWith("# ")) {
            out.append("<h1 style='color:#ffffff'>").append(inline(line.substring(2))).append("</h1>");
         } else if (line.startsWith("|")) {
            if (!line.matches("\\|[-| :]+\\|")) {
               String cells = line.replaceAll("^\\||\\|$", "").trim().replaceAll("\\s*\\|\\s*", "  /  ");
               out.append("<p style='margin:2px 0'>").append(inline(cells)).append("</p>");
            }
         } else {
            out.append("<p>").append(inline(line)).append("</p>");
         }
      }
      if (list) out.append("</ul>");
      if (code) out.append("</pre>");
      return out.append("</body></html>").toString();
   }

   private static String inline(String s) {
      return escape(s)
         .replaceAll("`([^`]+)`", "<code style='font-family:Consolas,monospace;color:#c9d4ff'>$1</code>")
         .replaceAll("\\*\\*([^*]+)\\*\\*", "<b>$1</b>")
         .replaceAll("\\[([^\\]]+)\\]\\(([^)]+)\\)", "<u>$1</u>");
   }

   private static String escape(String s) {
      return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
   }
}
