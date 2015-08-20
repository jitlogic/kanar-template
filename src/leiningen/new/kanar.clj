(ns leiningen.new.kanar
  (:use [leiningen.new.templates :only [renderer name-to-path ->files]]))

(def render (renderer "kanar"))

(def non-src-files
  [".gitignore" "LICENSE" "project.clj" "kanar.conf" "services.conf"])

(def src-files ["app.clj" "app/views.clj"])

(defn kanar
  "Create new Kanar project."
  [name]
  (let [data {:name name
              :sanitized (name-to-path name)}]
    (apply
      ->files
      (cons
        data
        (concat
          (for [f non-src-files] [f (render f data)])
          (for [f src-files] [(str "src/{{sanitized}}/" f) (render (str "src/" f) data)]))
        ))))
