// -*- js2 -*-


// I think by the time kill_buffer_hook runs the buffer is gone so I
// patch kill_buffer

var kill_buffer_original = kill_buffer_original || kill_buffer;

var killed_buffer_urls = [];

kill_buffer = function (buffer, force) {
    if (buffer.display_uri_string) {
        killed_buffer_urls.push(buffer.display_uri_string);
    }

    kill_buffer_original(buffer,force);
};

interactive("restore-killed-buffer-url", "Loads url from a previously killed buffer",
    function restore_killed_buffer_url (I) {
        if (killed_buffer_urls.length !== 0) {
            var url = yield I.minibuffer.read(
                $prompt = "Restore killed url:",
                $completer = new all_word_completer($completions = killed_buffer_urls),
                $default_completion = killed_buffer_urls[killed_buffer_urls.length - 1],
                $auto_complete = "url",
                $auto_complete_initial = true,
                $auto_complete_delay = 0,
                $require_match = true);

            load_url_in_new_buffer(url);
        } else {
            I.window.minibuffer.message("No killed buffer urls");
        }
    });


define_webjump("sitebag", "javascript:%28function%28%29%7Bif%28%22bag.eknet.org%22%21%3Ddocument.location.host%29%7Bvar%20a%3D%22https://bag.eknet.org/api/eike/entry%3Fadd%26url%3D%22%2BencodeURIComponent%28location.href%29%2Cb%3Ddocument.createElement%28%22script%22%29%3Bb.setAttribute%28%22type%22%2C%22text/javascript%22%29%2Cb.setAttribute%28%22charset%22%2C%22utf-8%22%29%2Cb.setAttribute%28%22src%22%2Ca%29%2Cdocument.documentElement.appendChild%28b%29%7D%7D%29%28%29%3B");

define_webjump("github", "https://github.com/search?utf8=%E2%9C%93&q=%s");
define_webjump("dict.cc", "http://www.dict.cc/?s=%s");

homepage = "about:blank";

url_remoting_fn = load_url_in_new_buffer;

cwd = get_home_directory();
cwd.append("Downloads");

external_content_handlers.set("application/pdf", "xdg-open");

editor_shell_command = "emacsclient -c";

xkcd_add_title = true;

url_completion_use_history = true;
session_pref('browser.history_expire_days', 5);

require("duckduckgo");

add_hook("mode_line_hook", mode_line_adder(buffer_count_widget), true);

load_paths.unshift("chrome://conkeror-contrib/content/");
require("mode-line-buttons.js");
mode_line_add_buttons(standard_mode_line_buttons, true);

require("favicon");
add_hook("mode_line_hook", mode_line_adder(buffer_icon_widget), true);
read_buffer_show_icons = true;

dowload_buffer_automatic_open_target = OPEN_NEW_BUFFER_BACKGROUND;

function dlm_fetch (url, open) {
    var cmd = "dlm fetch '" + url + "'";
    if (open) {
        cmd = cmd + " && xdg-open \"$(dlm query -Hs '" + url + "' short)\"";
    }
    shell_command_blind(cmd);
};

function dlm_download (open) {
    return function(I) {
        var mb = I.window.minibuffer;
        bo = yield read_browser_object(I);
        link = load_spec_uri_string(load_spec(bo));
        mb.message("Downloading " + link);
        dlm_fetch(link, open);
    };
}

interactive("dlm-download",
    "Download the url using dlm.",
    alternates(dlm_download(true), dlm_download(false)),
    $browser_object = browser_object_links);

define_key(content_buffer_normal_keymap, "C-d", "dlm-download");


if ('@hyperstruct.net/mozlab/mozrepl;1' in Cc) {
  var mozrepl = Cc['@hyperstruct.net/mozlab/mozrepl;1'].getService(Ci.nsIMozRepl);
  if (! mozrepl.isActive())
    mozrepl.start(4243);
}

add_hook('create_buffer_hook_late', function(buffer) {
   browser_zoom_set(buffer, true, 150);
});
