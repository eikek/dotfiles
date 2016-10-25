{:user
 {:plugins [[lein-exec "0.3.6"]
            [com.aphyr/prism "0.1.2"]]
  :dependencies [[com.aphyr/prism "0.1.2"]
                 [pjstadig/humane-test-output "0.8.1"]]
  :injections [(require 'pjstadig.humane-test-output)
               (pjstadig.humane-test-output/activate!)]}}
