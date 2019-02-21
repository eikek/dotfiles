## Documentation:
##   qute://help/configuring.html
##   qute://help/settings.html

import socket

c.backend = 'webengine'
c.tabs.show = 'never' # was: switching
c.content.autoplay = False

c.content.cookies.accept = 'no-3rdparty'

c.content.default_encoding = 'utf-8'

c.content.headers.accept_language = 'de-DE,de,en-US,en'
c.content.headers.custom = {}
c.content.headers.do_not_track = True
c.content.headers.referer = 'same-domain'
c.content.notifications = False
c.content.pdfjs = False
c.content.user_stylesheets = []

c.editor.command = ['emacsclient', '-c', '{file}']
c.editor.encoding = 'utf-8'

c.hints.chars = 'uiaeosnrtd'

c.input.insert_mode.auto_enter = False
c.input.insert_mode.auto_leave = False

#   - tab: Open a new tab in the existing window and activate the window.
#   - tab-bg: Open a new background tab in the existing window and activate the window.
#   - tab-silent: Open a new tab in the existing window without activating the window.
#   - tab-bg-silent: Open a new background tab in the existing window without activating the window.
#   - window: Open in a new window.
c.new_instance_open_target = 'tab'

c.url.default_page = 'https://start.duckduckgo.com/'
c.url.searchengines = {
    "DEFAULT": "https://duckduckgo.com/?q={}",
    "github": "https://github.com/search?q={}",
    "scala": "https://www.scala-lang.org/api/2.12.8/?search={}",
    "dict": "https://www.dict.cc/?s={}"
}

c.window.hide_decoration = True

c.bindings.key_mappings = {'<Ctrl-G>': '<Escape>', '<Ctrl-M>': '<Return>', '<Enter>': '<Return>', '<Ctrl-Enter>': '<Ctrl-Return>'}

# emacs like key bindings
# starting help here: https://gitlab.com/jgkamat/qutemacs/blob/master/qutemacs.py
# c.input.insert_mode.auto_enter = True
# c.input.insert_mode.auto_leave = False
# c.input.insert_mode.plugins = False

# Forward unbound keys
c.input.forward_unbound_keys = "all"

ESC_BIND = 'clear-keychain ;; search ;; fullscreen --leave'

c.bindings.default['normal'] = {}
# Bindings
c.bindings.commands['normal'] = {
    # Navigation
    '<ctrl-v>': 'scroll-page 0 0.5',
    '<alt-v>': 'scroll-page 0 -0.5',
    '<ctrl-shift-v>': 'scroll-page 0 1',
    '<alt-shift-v>': 'scroll-page 0 -1',
    '<alt-n>': 'tab-next',
    '<alt-p>': 'tab-prev',
    '<alt-r>': 'reload',

    # Zoom
    '<ctrl++>': 'zoom-in',
    '<ctrl+->': 'zoom-out',

    # Commands
    '<alt-x>': 'set-cmd-text :',
    ':': 'set-cmd-text :',
    '<ctrl-x>b': 'set-cmd-text -s :buffer',
    '<ctrl-x>k': 'close',
    'q': 'tab-close',
    '<ctrl-x><ctrl-c>': 'quit',
    '<ctrl-x><ctrl-f>': 'set-cmd-text -s :open -t',
    'G': 'set-cmd-text :open {url:pretty}',
    'g': 'set-cmd-text -s :open -r ',
    'yy': 'yank',
    'yh': 'hint links yank',
    
    # searching
    '<ctrl-s>': 'set-cmd-text /',
    '<ctrl-r>': 'set-cmd-text ?',

    # hinting
    'f': 'hint',
    't': 'hint inputs',
#    ';I': 'hint images tab',
    ';O': 'hint links fill :open -t -r {hint-url}',
#    ';R': 'hint --rapid links window',
#    ';Y': 'hint links yank-primary',
#    ';b': 'hint all tab-bg',
    ';d': 'hint links download',
#    ';f': 'hint all tab-fg',
    ';h': 'hint all hover',
    ';i': 'hint images',
    ';o': 'hint links fill :open {hint-url}',
    ';r': 'hint --rapid links tab-bg',

    # history
    '<ctrl-]>': 'forward',
    '<ctrl-[>': 'back',
    'l': 'back',
    'B': 'back',
    'F': 'forward',

    # editing
    '<ctrl-f>': 'fake-key <Right>',
    '<ctrl-b>': 'fake-key <Left>',
    '<': 'fake-key <Home>',
    '>': 'fake-key <End>',
    '<ctrl-n>': 'fake-key <Down>',
    '<ctrl-p>': 'fake-key <Up>',
    '<alt-f>': 'fake-key <Ctrl-Right>',
    '<alt-b>': 'fake-key <Ctrl-Left>',
    '<ctrl-d>': 'fake-key <Delete>',
    '<alt-d>': 'fake-key <Ctrl-Delete>',
    '<alt-backspace>': 'fake-key <Ctrl-Backspace>',
    '<ctrl-w>': 'fake-key <Ctrl-backspace>',
    '<ctrl-y>': 'insert-text {clipboard}',
    '<ctrl-e>': 'enter-mode insert',

    # Numbers
    # https://github.com/qutebrowser/qutebrowser/issues/4213
    '1': 'fake-key 1',
    '2': 'fake-key 2',
    '3': 'fake-key 3',
    '4': 'fake-key 4',
    '5': 'fake-key 5',
    '6': 'fake-key 6',
    '7': 'fake-key 7',
    '8': 'fake-key 8',
    '9': 'fake-key 9',
    '0': 'fake-key 0',

    # escape hatch
    '<ctrl-h>': 'set-cmd-text -s :help',
    '<ctrl-g>': ESC_BIND,
}

c.bindings.commands['command'] = {
    '<ctrl-s>': 'search-next',
    '<ctrl-r>': 'search-prev',

    '<ctrl-p>': 'completion-item-focus prev',
    '<ctrl-n>': 'completion-item-focus next',

    '<alt-p>': 'command-history-prev',
    '<alt-n>': 'command-history-next',

    # escape hatch
    '<ctrl-g>': 'leave-mode',
}

c.bindings.commands['hint'] = {
    # escape hatch
    '<ctrl-g>': 'leave-mode',
}


c.bindings.commands['caret'] = {
    # escape hatch
    '<ctrl-g>': 'leave-mode',
}

c.bindings.commands['insert'] = {
    '<ctrl-y>': 'insert-text {clipboard}',
}

if socket.gethostname() == "kythira":
    c.zoom.default = '150%'
else:
    c.zoom.default = '100%'
    
