(ns atm-machine.storage.transactiondao
  (:require [clojure.java.jdbc :as db]
            [atm-machine.model.person :refer [person-desc]]
            [atm-machine.model.transaction :refer [transaction-desc]]
            [atm-machine.storage.persondao :as persondao]
            [atm-machine.utils.statement-helper :as statement-helper]))


(defn get-balance [spec ag acc]
  (let [person-id (persondao/get-id spec ag acc)]
  (let [rows (db/query spec [(str "SELECT * FROM " (:table-name transaction-desc) " WHERE "
                                  (:account transaction-desc) " = " person-id " ORDER BY "
                                  (:transaction-time transaction-desc) " DESC LIMIT 1")])]
    (if (empty? rows) 0 ((keyword (:balance transaction-desc)) (first rows))))))


(defn get-statement [spec ag acc days]
  (let [person-id (persondao/get-id spec ag acc)]
  (let [rows (db/query spec [(str "SELECT * FROM " (:table-name transaction-desc) " WHERE "
                                  (:account transaction-desc) " = " person-id " AND TO_CHAR (CURRENT_TIMESTAMP - interval '"
                                  days " days', 'YYYYMMDD') <= TO_CHAR ("
                                  (:transaction-time transaction-desc) ", 'YYYYMMDD')")])]
  (let [parsed-rows (statement-helper/convert-all-transactions rows)]
    (statement-helper/build-statement-map parsed-rows)))))


(defn perform-operation! [spec ag acc value description]
  (db/with-db-transaction [trans-conn spec]
    (let [person-id (persondao/get-id trans-conn ag acc)]
    (let [balance (get-balance trans-conn ag acc)]
    (let [new-balance (if (nil? balance) 0 (+ balance value))]
    (if (< new-balance 0)
      nil
      (let [insert-query (str "INSERT INTO " (:table-name transaction-desc) " ("
                              (:account transaction-desc) ", "
                              (:balance transaction-desc) ", "
                              (:value transaction-desc) ", "
                              (:description transaction-desc) ", "
                              (:transaction-time transaction-desc) ") VALUES ("
                              person-id ", "
                              new-balance ", "
                              value ", '"
                              description "', "
                              "CURRENT_TIMESTAMP)")]
        (db/execute! trans-conn [insert-query]))))))))

(defn transfer! [spec from-ag from-acc to-ag to-acc value]
  (db/with-db-transaction [trans-conn spec]
    (let [from-id (persondao/get-id trans-conn from-ag from-acc)
          to-id (persondao/get-id trans-conn to-ag to-acc)]
      (if (< (get-balance trans-conn from-ag from-acc) value)
        nil
        (do (perform-operation! trans-conn from-ag from-acc (- value) (str "Transference to Ag:" to-ag ", Acct: " to-acc))
         (perform-operation! trans-conn to-ag to-acc value (str "Transference from Ag:" from-ag ", Acct: " from-acc)))))))


(defn create-transaction-table! [spec]
  (db/execute! spec [(str "CREATE TABLE IF NOT EXISTS " (:table-name transaction-desc) " ("
                              (:id transaction-desc) " BIGSERIAL PRIMARY KEY, "
                              (:account transaction-desc) " BIGINT NOT NULL REFERENCES " (:table-name person-desc) ", "
                              (:balance transaction-desc) " BIGINT NOT NULL, "
                              (:value transaction-desc) " BIGINT NOT NULL, "
                              (:description transaction-desc) " TEXT NOT NULL, ",
                              (:transaction-time transaction-desc) " TIMESTAMP NOT NULL)")]))
