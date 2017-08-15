(ns atm-machine.storage.transactionsdao)

(defprotocol TransactionDAO
    (transfer! [storage from to value])
    
    (get-balance [storage])

    (get-statement [storage last-days])

    (perform-operation! [storage value description]))

(def transaction-desc {:table-name "public.transaction"
                    :id "id"
                    :account "account"
                    :balance "balance"
                    :value "value"
                    :description "description"
                    :transaction-time "transaction-time"})

(defn create-transaction-table! [spec]
  (db/execute! spec [(str "CREATE TABLE IF NOT EXISTS " (:table-name transaction-desc) "( "
                              (:id transaction-desc) " BIGSERIAL PRIMARY KEY, "
                              (:account transaction-desc) " BIGINT NOT NULL REFERENCES " (:table-name persondao.person-desc) ", "
                              (:balance transaction-desc) " BIGINT NOT NULL, "
                              (:value transaction-desc) " BIGINT NOT NULL, "
                              (:description transaction-desc) " TEXT NOT NULL ",
                              (:transaction-time transaction-desc) " TIMESTAMP NOT NULL)")]))