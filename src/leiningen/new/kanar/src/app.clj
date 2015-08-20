(ns {{name}}.app
  (:gen-class)
  (:require
    [compojure.core :refer [routes GET ANY rfn]]
    [compojure.route :refer [not-found]]
    [clojure.tools.nrepl.server :as nrepl]
    [ring.middleware.reload :refer [wrap-reload]]
    [ring.middleware.keyword-params :refer [wrap-keyword-params]]
    [ring.middleware.params :refer [wrap-params]]
    [ring.middleware.cookies :refer [wrap-cookies]]
    [ring.util.response :refer [redirect]]
    [org.httpkit.server :refer [run-server]]
    [kanar.core :as kc]
    [kanar.core.ticket :as kt]
    [kanar.ldap :as kl]
    [kanar.core.util :as ku]
    [clj-ldap.client :as ldap]
    [ring.adapter.jetty :refer [run-jetty]]
    [{{name}} .app.views :as kav]
    [taoensso.timbre :as log])
  )

; TODO configuration load and

; ;;;;;;;;;;;;;;;;;;;;;;;;;;;; Standard implementation of authentication functions


(defn kanar-routes-new [app-state]
  (routes
    (ANY "/login" req                                       ; TODO ANY -> POST/GET
      (kc/login-handler (:form-login-flow @app-state) @app-state req))
    (ANY "/logout" req
      (kc/logout-handler @app-state req))
    (ANY "/validate" req
      (kc/cas10-validate-handler @app-state req))
    (ANY "/serviceValidate" req
      (kc/cas20-validate-handler @app-state req #"ST-.*"))
    (ANY "/proxyValidate" req
      (kc/cas20-validate-handler @app-state req #"(ST|PT)-.*"))
    (ANY "/proxy" req
      (kc/proxy-handler @app-state req))
    (ANY "/samlValidate" req
      (kc/saml-validate-handler @app-state req))
    (ANY "/*" []
      (redirect "login"))))



(def DEFAULT-CONFIG
  {
   :server-id "SVR1"                                        ; name appended to generated ticket IDs
   :nrepl-enabled false                                     ; enable NREPL port
   :nrepl-port 7700                                         ; NREPL port
   :http-enabled true                                       ; enable HTTP
   :http-port 8080                                          ; HTTP port
   :https-enabled true                                      ; enable HTTPS
   :https-port 8443                                         ; HTTPS port
   })


(defn new-app-state [old-app-state conf services]
  (let [ldap-conn (ldap/connect (:ldap-conf conf))
        auth-fn (ku/chain-auth-fn
                  (kl/ldap-auth-fn ldap-conn (:ldap-conf conf) [])
                  (kl/ldap-attr-fn ldap-conn {:sn :sn, :dn :dn, :givenName :givenName, :cn :cn}))]
    {:ticket-seq          (or (:ticket-seq old-app-state) (atom 0))
     :conf                (into DEFAULT-CONFIG conf)
     :service             services
     :ticket-registry     (or (:ticket-registry old-app-state) (kt/atom-ticket-registry (atom {}) (:server-id conf)))
     :render-message-view kav/message-view
     :form-login-flow     (kc/form-login-flow auth-fn kav/login-view)
     }))


(defonce ^:dynamic *app-state* (atom {}))

(defn wrap-error-screen [f]
  (fn [req]
    (try
      (f req)
      (catch Throwable e
        (log/error "Fatal error: " e)
        (.printStackTrace e)
        {:status  200
         :headers {"Content-Type" "text/html; charset=utf-8"}
         :body    (kav/message-view :error "Unexpected error.")}))))


;(swap! *app-state* #(new-app-state % (into DEFAULT-CONFIG @*app-conf*) @*services*))

(defn reload
  ([] (reload (System/getProperty "kanar.home")))
  ([homedir]
   (let [conf (read-string (slurp (str homedir "/kanar.conf")))
         svcs (read-string (slurp (str homedir "/services.conf")))]
     (swap! *app-state* #(new-app-state % (into DEFAULT-CONFIG conf) svcs))
     :ok
     )))


(def kanar-routes (kanar-routes-new *app-state*))

(def kanar-handler
  (wrap-reload
    (-> #'kanar-routes
        wrap-error-screen
        wrap-cookies
        wrap-keyword-params
        wrap-params)))


;;;;;;;;;;;;;;;;;;;; Start and stop functions

(defonce stopf (atom nil))
(defonce repl-server (atom nil))
(defonce ticket-cleaner-f (atom nil))


(defn stop-server []
  (when-let [f @stopf]
    (.stop f))
  (when-let [cf @ticket-cleaner-f]
    (future-cancel cf)))


(defn start-server []
  (stop-server)
  (reload)
  (let [{:keys [http-port https-port https-keystore https-keypass nrepl-port]} (:conf @*app-state*)]
    (reset! stopf (run-jetty kanar-handler
                             {:port http-port :join? false :ssl? true :ssl-port https-port
                              :keystore https-keystore :key-password https-keypass }))
    (reset! ticket-cleaner-f (kc/ticket-cleaner-task *app-state*))
    (when-not @repl-server
      (reset! repl-server (nrepl/start-server :bind "0.0.0.0" :port nrepl-port)))))

(defn restart []
  (stop-server)
  (Thread/sleep 100)
  (start-server))


(defn -main [& args]
  (println "Starting KANAR server.")
  (start-server))



