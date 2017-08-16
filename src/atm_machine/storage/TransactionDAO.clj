(ns atm-machine.storage.transactionsdao
  (:require [clojure.java.jdbc :as db]
            [atm-machine.model.person :refer [person-desc]]
            [atm-machine.model.transaction :refer [transaction-desc]]))

(defprotocol TransactionDAOProtocol
    (transfer! [storage from to value])

    (get-balance [storage ag acc])

    (get-statement [storage last-days])

    (perform-operation! [storage ag acc value description]))


(defn get-balance [spec ag acc]
  (let [person-id (persondao/get-id spec ag acc)]
  (let [rows (db/query spec [(str "SELECT * FROM " (:table-name transaction-desc) " WHERE "
                                  (:account transaction-desc) " = " person-id " ORDER BY "
                                  (:transaction-time transaction-desc) " DESC LIMIT 1")])]
    (if (empty? rows) nil ((:balance transaction-desc) (first rows))))))




(defn get-statement [spec ag acc days]
  (let [person-id (persondao/get-id spec ag acc)]
  (let [rows (db/query spec [(str "SELECT * FROM " (:table-name transaction-desc) " WHERE "
                                  (:account transaction-desc) " = " person-id " AND TO_CHAR (CURRENT_TIMESTAMP - interval '"
                                  days " days', 'YYYYMMDD') <= " (:transaction-time transaction-desc))])]
    rows
    )))

(defn perform-operation! [spec ag acc value description]
  (db/with-db-transaction [trans-conn spec]
    (let [person-id (persondao/get-id trans-conn ag acc)]
    (let [balance (get-balance trans-conn ag acc)]
    (let [new-balance (if (nil? balance) 0 (+ balance value))]
    (let [insert-query (str "INSERT INTO " (:table-name transaction-desc) " ("
                            (:account transaction-desc) ", "
                            (:balance transaction-desc) ", "
                            (:value transaction-desc) ", "
                            (:description transaction-desc) ", "
                            (:transaction-time transaction-desc) ") VALUES ("
                            person-id ", "
                            new-balance ", "
                            value ", "
                            description ", "
                            "CURRENT_TIMESTAMP)")]
      (db/execute! trans-conn [insert-query])))))))

(defn transfer! [spec from-ag from-acc to-ag to-acc value]
  (db/with-db-transaction [trans-conn spec]
    (let [from-id (persondao/get-id trans-conn from-ag from-acc)
          to-id (persondao/get-id trans-conn to-ag to-acc)]
    (let [from-row-query (str "SELECT * FROM " (:table-name transaction-desc) " WHERE "
                              (:id transaction-desc) " = " from-id)]
    (let [from-row (first (db/query trans-conn from-row-query))]
      (if (< ((:value transaction-desc) from-row) value)
        nil
        ((perform-operation! trans-conn from-ag from-acc (- value) (str "Transference to Ag:" to-ag ", Acct: " to-acc))
         (perform-operation! trans-conn to-ag to-acc value (str "Transference from Ag:" from-ag ", Acct: " from-acc)))))))))


(defn create-transaction-table! [spec]
  (db/execute! spec [(str "CREATE TABLE IF NOT EXISTS " (:table-name transaction-desc) "( "
                              (:id transaction-desc) " BIGSERIAL PRIMARY KEY, "
                              (:account transaction-desc) " BIGINT NOT NULL REFERENCES " (:table-name persondao/person-desc) ", "
                              (:balance transaction-desc) " BIGINT NOT NULL, "
                              (:value transaction-desc) " BIGINT NOT NULL, "
                              (:description transaction-desc) " TEXT NOT NULL ",
                              (:transaction-time transaction-desc) " TIMESTAMP NOT NULL)")]))
