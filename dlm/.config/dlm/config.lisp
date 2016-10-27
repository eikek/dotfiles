;; set a default lifetime in seconds
;(setf *file-lifetime* (* 5 24 60 60))

;; set the default fetch program. the ~a is replaced with the url
;(setf *fetch-program* "curl -O ~a")

;; set default target directory
(setf *default-target*
      "/home/eike/Downloads")

(setf *youtube-dl-bin* "/run/current-system/sw/bin/youtube-dl")

;; set a default command
;(setq *default-command* "query")

;; set the which command
;(setq *which-program* "/run/current-system/sw/bin/which")

;; add notification
(push (lambda (md existed)
        (when (and (not (getf md :error)) (not existed))
          (external-program:run
           "stumpish"
           `("echo" ,(format nil "Download ~a finished." (getf md :source))))))
      *download-notify-hook*)
