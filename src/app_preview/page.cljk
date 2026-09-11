(ns app-preview.page
  "Preview's window: the pages listed, and the selected one drawn.

  ## Drawing is `hanmen`'s and decoding is still nobody's here

  `app-preview.model` decides what a page IS and `app-preview.source` is the
  `fs/browse` seam. Neither reads a file, and this does not either: a host
  that has spent the grant hands over `hanmen.page` values — a size, a
  rotation, placed marks — and this draws them.

  That keeps the capability story intact while closing the gap it left. The
  listing said a page was 595×842 with 2,587 characters of text and could not
  show a single one of them, which is the same shape of answer ADR-0007
  called out in a different app: the fields of the value are not the value.

  `hanmen.svg` is required for the drawing only. Nothing here parses
  anything, and the fragment it produces loads nothing unless the host passes
  an `:image-href` — which a host that has not decided its CSP should not."
  (:require [app-preview.model :as model]
            [hanmen.page :as hanmen]
            [hanmen.svg :as hanmen-svg]
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

(defn- selected-index
  "The index of the selected page, or nil.

  The item id IS the page index — `model/entry->item` chose that over the
  printed label because a document with front matter has two pages both
  labelled `i`. So this is a lookup and not a search."
  [view]
  (first (:selection/ids (:view/selection view))))

(defn detail
  "The selected page, drawn, or nil when there is nothing to draw.

  `pages` is index → `hanmen.page` value. A page the host did not render is
  nil here, and nil means the slot adds nothing — a blank card under the
  toolbar reads as something that failed to load."
  [pages opts]
  (fn [view]
    (when-let [p (get pages (selected-index view))]
      [:figure {:class "app-preview__page"}
       (hanmen-svg/emit p (select-keys opts [:image-href]))
       (when (hanmen/scanned? p)
         ;; The same sentence the empty search state gives, in the place
         ;; somebody is looking when they wonder why selecting text does
         ;; nothing.
         [:figcaption {:class "hig-footnote"}
          "このページには抽出できるテキストがありません（スキャン画像）。"])])))

(def page-css
  "What the drawn page needs beyond `hanmen.svg/stylesheet`.

  Bounded and scrollable: a page is taller than a window and a figure that
  grows to fit one pushes the listing off the screen. Tokens only — the
  paper is a token too, because `hanmen` paints in `currentColor` and picks
  no colour of its own, which is what makes one rendering right in both
  light and dark."
  (str ".app-preview__page{margin:0;max-height:60vh;overflow:auto;"
       "border-radius:var(--hig-radius-2);"
       "background:var(--hig-background-secondary)}"
       ".app-preview__page .hanmen-page{--hanmen-paper:var(--hig-background-primary)}"))

(defn render
  "The window. `opts` may carry `:pages` (index → `hanmen.page`) and
  `:image-href`; without them this is the listing it always was."
  ([cat] (render cat {}))
  ([cat opts]
   (mui/->page (catalog/view cat)
               (assoc view-opts
                      :detail (detail (:pages opts) opts)
                      :extra-css (str hanmen-svg/stylesheet page-css)))))

(defn render-html
  ([cat] (render-html cat {}))
  ([cat opts]
   (mui/->html (catalog/view cat)
               (assoc view-opts :detail (detail (:pages opts) opts)))))

(defn coverage [cat]
  (model/text-coverage (:catalog/items cat)))
