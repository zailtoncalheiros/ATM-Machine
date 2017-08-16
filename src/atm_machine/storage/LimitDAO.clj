(ns atm-machine.storage.transactionsdao
  (:require [atm-machine.model.person :refer [person-desc]]
            [atm-machine.model.limit :refer [limit-desc]]))

(defprotocol LimitDAO
    (update-limit! [storage value]))

(defn create-limit-table! [spec]
  (db/execute! spec [(str "CREATE TABLE IF NOT EXISTS " (:table-name limit-desc) "("
                              (:id limit-desc) " BIGSERIAL PRIMARY KEY,"
                              (:account limit-desc) " BIGINT NOT NULL REFERENCES " (:table-name persondao/person-desc) ", "
                              (:value limit-desc) " BIGINT NOT NULL, "
                              (:last-update limit-desc) " TIMESTAMP NOT NULL)")]))
