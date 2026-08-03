(ns app-preview.source
  "The `fs/browse` seam. Decoding is kasane's job and the provider's."
  (:require [app-preview.model :as model]
            [mokuroku.source :as source]))

(defrecord PageSource [title read-fn]
  source/ISource
  (-descriptor [_] (model/descriptor title))
  (-fetch [_] (model/listing->items (read-fn title))))

(defn page-source [title read-fn] (->PageSource title read-fn))
(defn fixture-source [title entries] (page-source title (constantly entries)))

(def denied
  {:preview/state :denied :preview/capability model/capability :preview/pages []})

(defn granted [pages]
  {:preview/state :granted :preview/capability model/capability
   :preview/pages (vec pages)})

(defn denied? [r] (= :denied (:preview/state r)))

(def undecodable
  "The file was readable but nothing here could parse it.

  Distinct from a refused grant and from an empty document: the fix is a
  decoder, not a permission, and telling the user to check permissions when
  the format is simply unsupported sends them somewhere useless."
  {:preview/state :undecodable :preview/capability model/capability})

(defn undecodable? [r] (= :undecodable (:preview/state r)))
