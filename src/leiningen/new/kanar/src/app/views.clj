(ns {{name}}.app.views
  (:require
    [hiccup.core :refer [html]]))


(defn login-view [& {:keys [username error-msg service TARGET]}]
  (html
    [:html
     [:head [:title "Kanar Login"]]
     [:body
      (if error-msg
        [:div error-msg])
      [:form {:method :post :action :login}
       [:div
        [:span "Username"]
        [:input#username {:type :text :autocomplete :false :size "25"
                          :value (or username "") :tabindex "1" :name :username}]
        [:span "Password"]
        [:input#password {:type :password :autocomplete :false :size "25"
                          :value "" :tabindex "1" :name :password}]
        [:input#lt {:type :hidden :name :lt :value "lt" }]
        (if service [:input#service {:type :hidden :name :service :value service}])
        (if TARGET [:input#TARGET {:type :hidden :name :TARGET :value TARGET}])
        [:input {:type :submit :value :submit}]]]
      ]]))


(defn su-login-view [& {:keys [username runas error-msg service TARGET]}]
  (html
    [:html
     [:head [:title "Kanar Login"]]
     [:body
      (if error-msg
        [:div error-msg])
      [:form {:method :post :action :login}
       [:div
        [:span "Username"]
        [:input#username {:type :text :autocomplete :false :size "25"
                          :value (or username "") :tabindex "1" :name :username}]
        [:span "Password"]
        [:input#password {:type :password :autocomplete :false :size "25"
                          :value "" :tabindex "3" :name :password}]
        [:span "Run as:"]
        [:input#runas {:type :text :autocomplete :false :size "25"
                       :value (or runas "") :tabindex "3" :name :runas}]
        [:input#lt {:type :hidden :name :lt :value "lt" }]
        (if service [:input#service {:type :hidden :name :service :value service}])
        (if TARGET [:input#TARGET {:type :hidden :name :TARGET :value TARGET}])
        [:input {:type :submit :value :submit}]]]
      ]]))


(defn message-view [_ msg & {:keys [url link]}]
  (html
    [:html
     [:head [:title "Kanar"]]
     [:body
      [:div msg]
      (if url [:a {:href url} link])]
     ]))

