(ns atm-machine.storage.transactionsdao
  (:require [atm-machine.storage.persondao :as persondao]))

(defprotocol LimitDAO
    (update-limit! [storage value]))

(def limit-desc {:table-name "public.limit"
                    :id "id"
                    :account "account"
                    :value "value"
                    :last-update "last_udpate"})

(defn create-limit-table! [spec]
  (db/execute! spec [(str "CREATE TABLE IF NOT EXISTS " (:table-name limit-desc) "("
                              (:id limit-desc) " BIGSERIAL PRIMARY KEY,"
                              (:account limit-desc) " BIGINT NOT NULL REFERENCES " (:table-name persondao.person-desc) ", "
                              (:value limit-desc) " BIGINT NOT NULL, "
                              (:last-update limit-desc) " TIMESTAMP NOT NULL)")]))