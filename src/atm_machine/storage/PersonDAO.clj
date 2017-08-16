(ns atm-machine.storage.persondao
    (:require [clojure.java.jdbc :as db]
              [atm-machine.model.person :refer [person-desc]]))

(defprotocol PersonDAOProtocol
    (put-person! [storage person])

    (authentic? [storage agency account password])

    (get-id [storage agency account]))


(defn put-person! [spec person]
  (let [sql-query (str "INSERT INTO " (:table-name person-desc) " ("
                       (:fullname person-desc) ", "
                       (:cpf person-desc) ", "
                       (:address person-desc) ", "
                       (:date-of-birth person-desc) ", "
                       (:password person-desc) ", "
                       (:agency person-desc) ", "
                       (:account person-desc) ") VALUES ('"
                       (:fullname person) "', '"
                       (:cpf person) "', '"
                       (:address person) "', '"
                       (:date-of-birth person) "', '"
                       (:password person) "', "
                       "(9999*random())::integer, "
                       "(99999*random())::integer)")]
    (db/execute! spec [sql-query])))

(defn authentic? [spec agency account password]
  (let [sql-query (str "SELECT * FROM " (:table-name person-desc) " WHERE "
                       (:agency person-desc) " = " agency " AND "
                       (:account person-desc) " = " account)]
    (let [person-list (db/query spec [sql-query])]
      (if (nil? person-list) false (= password ((:password person-desc) (first person-list)))))))


(defn get-id [spec agency account]
  (let [sql-query (str "SELECT * FROM " (:table-name person-desc) " WHERE "
                       (:agency person-desc) " = " agency " AND "
                       (:account person-desc) " = " account)]
    (let [person-list (db/query spec [sql-query])]
      (if (nil? person-list) nil ((:id person-desc) (first person-list))))))

(defn create-person-table! [spec]
  (db/execute! spec [(str "CREATE TABLE IF NOT EXISTS public.person ("
                            (:id person-desc) " BIGSERIAL PRIMARY KEY, "
                            (:fullname person-desc) " TEXT NOT NULL, "
                            (:cpf person-desc) " TEXT NOT NULL, "
                            (:address person-desc) " TEXT NOT NULL, "
                            (:date-of-birth person-desc) " TEXT NOT NULL, "
                            (:password person-desc) " TEXT NOT NULL, "
                            (:agency person-desc) " INTEGER NOT NULL, "
                            (:account person-desc) " INTEGER NOT NULL, "
                            "UNIQUE (" (:agency person-desc) ", " (:account person-desc) ")"
                          ")" )]))
