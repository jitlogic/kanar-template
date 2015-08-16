(ns leiningen.new.kanar
  (:use [leiningen.new.templates :only [renderer name-to-path ->files]]))

(def render (renderer "kanar"))

(defn kanar
  "Create new Kanar project."
  [name]
  (let [data {:name name
              :sanitized (name-to-path name)}]
    (->files data
             ["src/{{sanitized}}/app.clj" (render "app.clj" data)])))
