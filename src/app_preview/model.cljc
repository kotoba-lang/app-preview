(ns app-preview.model
  "Preview's domain: the pages of one document.

  One capability, `fs/browse`, to reach the file. Decoding it is
  `kotoba-lang/kasane`'s job (PDF/PSD/AI) and the provider's; this namespace
  decides what a page is and how a page list behaves."
  (:require [mokuroku.item :as item]
            [mokuroku.source :as source]))

(def capability "fs/browse")

(def columns
  [(source/attribute :label "Page" :string)
   (source/attribute :number "No." :number)
   (source/attribute :dimensions "Size" :string)
   (source/attribute :text-chars "Text" :number)
   (source/attribute :rotation "Rotation" :number)])

(def commands
  "Read-only. No annotate, no sign, no export-with-changes: a viewer that can
  rewrite the document it is showing is a different program, and no provider
  here can write."
  #{:quicklook :copy-path :export})

(defn descriptor
  ([] (descriptor "Document"))
  ([title]
   (source/descriptor
    {:id :app-preview/pages
     :item-kind :page
     :label title
     :capability capability
     :commands commands
     :attributes columns})))

(defn displayed-size
  "Width and height after the page's rotation.

  A PDF page carries a rotation independent of its media box. Reporting the
  unrotated box shows a landscape page as portrait, and every thumbnail grid
  lays it out wrongly."
  [{:keys [width height rotation]}]
  (when (and (number? width) (number? height))
    (if (#{90 270 -90} rotation)
      [height width]
      [width height])))

(defn entry->item
  "The id is the page index, which is stable, rather than the printed page
  label, which is not: a document with front matter has two pages both
  labelled `i`, and a roman-numbered preface restarts at 1."
  [{:keys [index label text-chars rotation] :as entry}]
  (let [[w h] (displayed-size entry)]
    (item/item index
               :page
               (or label (str "Page " (inc index)))
               (cond-> {:label (or label (str "Page " (inc index)))
                        :number (inc index)
                        :text-chars text-chars
                        :rotation (or rotation 0)}
                 (and w h) (assoc :dimensions (str w " × " h)
                                  :width w
                                  :height h)))))

(defn listing->items [entries]
  (mapv entry->item entries))

(def document-order
  "Pages read in document order. There is no other sensible default, and
  unlike every other app in this suite the user is not expected to re-sort —
  the column exists so the order can be *restored*, not so it can be changed."
  [[:number :asc]])

(def default-query
  {:query/sort document-order :query/text "" :query/filters []})

(defn scanned?
  "A page with no extractable text.

  Reported rather than hidden: it is why a search over the document comes back
  empty, and without it the user concludes the search is broken."
  [it]
  (let [n (item/attr it :text-chars)]
    (or (nil? n) (zero? n))))

(defn searchable-pages [items]
  (vec (remove scanned? items)))

(defn text-coverage
  "How much of the document a text search can actually see."
  [items]
  (let [total (count items)
        searchable (count (searchable-pages items))]
    {:coverage/pages total
     :coverage/searchable searchable
     :coverage/scanned (- total searchable)
     :coverage/complete? (= total searchable)}))
