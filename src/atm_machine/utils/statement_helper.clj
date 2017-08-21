(ns atm-machine.utils.statement-helper
  (:require [atm-machine.model.transaction :refer [transaction-desc]]
            [clj-time.coerce :as coerce]
            [clj-time.format :as time-format]))

(def custom-formatter (time-format/formatter "yyyy-MM-dd"))

(defn convert-transaction [trans]
  (update trans (keyword (:transaction-time transaction-desc)) #(time-format/unparse custom-formatter (coerce/from-sql-time %))))

(defn convert-all-transactions [rows]
  (map convert-transaction rows))

;; separate-by-date receives a sorted list with dated transactions and separate them by date.
(defn separate-by-date [[f s & l]]
  (cond
    (nil? f) nil
    (nil? s) (list (list f))
    (= ((keyword (:transaction-time transaction-desc)) f) ((keyword (:transaction-time transaction-desc)) s))
      (let [ans-list (separate-by-date (cons s l))]
      (let [head (first ans-list) tail (rest ans-list)]
        (cons (cons f head) tail)))
    true (cons (list f) (separate-by-date (cons s l)))))


;; build-day-statement receives a list with all transactions for a given day and returns a map with statement descrption.
(defn build-day-statement [[h & t]]
  (letfn [(get-full-description [trans] (str ((keyword (:description transaction-desc)) trans) " " ((keyword (:value transaction-desc)) trans)))]
    (cond
      (nil? h) nil
      (nil? t) {:balance ((keyword (:balance transaction-desc)) h) :date ((keyword (:transaction-time transaction-desc)) h) :descriptions (list (get-full-description h))}
      true (update (build-day-statement t) :descriptions #(cons (get-full-description h) %)))))

(defn build-statement-map-aux [[h & t]]
  (cond
    (nil? h) nil
    true (cons (build-day-statement h) (build-statement-map-aux t))))

;; build-statement-map receives a list with transaction lists separated by day and returns a list with the statement descriptions.
(defn build-statement-map [param]
  (build-statement-map-aux (separate-by-date param)))
