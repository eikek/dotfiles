{:user
 {:plugins [[lein-exec "0.3.1"]
            [cider/cider-nrepl "0.9.1"]
            [com.aphyr/prism "0.1.2"]]
  :dependencies [[com.aphyr/prism "0.1.2"]
                 [pjstadig/humane-test-output "0.6.0"]]
  :injections [(require 'pjstadig.humane-test-output)
               (pjstadig.humane-test-output/activate!)]}}
