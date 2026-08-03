(ns app-preview.model-test
  (:require [app-preview.model :as model]
            [app-preview.page :as page]
            [app-preview.source :as source]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [design-quality.audit :as dq]
            [mokuroku.catalog :as catalog]
            [mokuroku.item :as item]))

(def pages
  [{:index 0 :label "i" :width 612 :height 792 :rotation 0 :text-chars 120}
   {:index 1 :label "ii" :width 612 :height 792 :rotation 0 :text-chars 0}
   {:index 2 :label "1" :width 792 :height 612 :rotation 90 :text-chars 3000}
   {:index 3 :width 612 :height 792 :text-chars nil}])

(defn- cat-of [ps]
  (catalog/refresh (catalog/catalog (source/fixture-source "Report.pdf" ps)
                                    model/default-query)))

(deftest identity-is-the-index-not-the-printed-label
  ;; A document with front matter has two pages both labelled "i", and a
  ;; roman-numbered preface restarts at 1.
  (let [dup [{:index 0 :label "i"} {:index 5 :label "i"}]
        items (model/listing->items dup)]
    (is (= 2 (count (set (map :item/id items)))))
    (is (= ["i" "i"] (mapv :item/label items)) "both really are labelled i")))

(deftest rotation-is-applied-before-the-size-is-reported
  ;; A PDF page carries a rotation independent of its media box. Reporting
  ;; the unrotated box shows a landscape page as portrait.
  (is (= [792 612] (model/displayed-size {:width 612 :height 792 :rotation 90})))
  (is (= [612 792] (model/displayed-size {:width 612 :height 792 :rotation 0})))
  (is (= [792 612] (model/displayed-size {:width 612 :height 792 :rotation 270})))
  (is (nil? (model/displayed-size {:rotation 90})) "no box, no size")

  (let [by-id (into {} (map (juxt :item/id identity)) (model/listing->items pages))]
    (is (= "612 × 792" (item/attr (by-id 0) :dimensions)))
    (is (= "612 × 792" (item/attr (by-id 2) :dimensions))
        "the 792×612 box rotated 90° displays as 612×792")))

(deftest an-unlabelled-page-is-numbered-from-one
  (let [it (model/entry->item {:index 3})]
    (is (= "Page 4" (:item/label it)))
    (is (= 4 (item/attr it :number)) "1-based, because that is what a reader counts")))

(deftest scanned-pages-are-reported-not-hidden
  ;; They are why a search over the document comes back empty. Without this
  ;; the user concludes the search is broken.
  (let [items (model/listing->items pages)
        c (model/text-coverage items)]
    (is (= 4 (:coverage/pages c)))
    (is (= 2 (:coverage/searchable c)))
    (is (= 2 (:coverage/scanned c)) "zero chars and nil chars both count as scanned")
    (is (false? (:coverage/complete? c))))

  (testing "a fully text document reports complete coverage"
    (is (true? (:coverage/complete?
                (model/text-coverage
                 (model/listing->items [{:index 0 :text-chars 10}])))))))

(deftest document-order-is-the-default
  (is (= [1 2 3 4] (mapv #(item/attr % :number)
                         (:result/items (catalog/result (cat-of pages)))))))

(deftest the-viewer-cannot-rewrite-the-document
  (let [c (catalog/select (cat-of pages) 0)]
    (is (= #{:quicklook :copy-path :export}
           (set (map :command/id (:view/commands (catalog/view c))))))
    (doseq [mutation [:rename :trash]]
      (is (= :source-does-not-accept (:proposal/refused (catalog/propose c mutation)))))))

(deftest undecodable-is-not-denied-and-not-empty
  ;; Telling the user to check permissions when the format is simply
  ;; unsupported sends them somewhere useless.
  (is (source/undecodable? source/undecodable))
  (is (not (source/denied? source/undecodable)))
  (is (not (source/undecodable? (source/granted pages)))))

(deftest the-window-says-why-a-search-found-nothing
  (let [html (page/render-html (catalog/search (cat-of pages) "zzzz"))]
    (is (str/includes? html "extractable text"))))

(deftest window-meets-the-design-quality-floor
  (let [ps {"document" (page/render (cat-of pages))
            "no-match" (page/render (catalog/search (cat-of pages) "zzzz"))
            "awaiting-grant" (page/render
                              (catalog/catalog (source/fixture-source "Report.pdf" [])
                                               model/default-query))}
        {:keys [overall pages] :as report} (dq/audit ps {:extra-axes dq/extra-axes})]
    (println "design-quality: aggregate" overall)
    (is (>= overall 98.0) (pr-str (:findings report)))
    (doseq [[nm r] pages] (is (>= (:overall r) 98.0) nm))))
