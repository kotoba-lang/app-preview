(ns app-preview.page
  (:require [app-preview.model :as model]
            [mokuroku.catalog :as catalog]
            [mokuroku-ui.core :as mui]))

(def view-opts
  {:columns [:label :number :dimensions]
   :formatters {:text-chars #(when % (str % " chars"))
                :rotation #(when % (str % "°"))}
   :noun "pages"
   :search-placeholder "Search this document"
   :empty-title "No pages"
   ;; Search over a scanned document returns nothing, and the user concludes
   ;; the search is broken. Naming the cause is the whole job of this line.
   :empty-body "No page matched. Pages with no extractable text cannot be searched."
   :badge (fn [it] (when (model/scanned? it) "No text"))
   :title "Preview"
   :description "The pages of a document, in document order."})

(defn render [cat] (mui/->page (catalog/view cat) view-opts))
(defn render-html [cat] (mui/->html (catalog/view cat) view-opts))

(defn coverage [cat]
  (model/text-coverage (:catalog/items cat)))
