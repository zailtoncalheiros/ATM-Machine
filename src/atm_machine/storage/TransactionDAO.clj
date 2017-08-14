(ns atm-machine.storage.transactionsdao)

(defprotocol TransactionDAO
    (transfer! [storage from to value])
    
    (get-balance [storage])

    (get-statement [storage last-days])

    (perform-operation! [storage value description]))

(defn create-transaction-table! [spec]
  (db/execute! spec [ "CREATE TABLE IF NOT EXISTS public.transaction
                            (
                              id BIGSERIAL PRIMARY KEY,
                              account bigint NOT NULL REFERENCES public.person,
                              balance bigint NOT NULL,
                              value bigint NOT NULL,
                              description text,
                              transaction_time TIMESTAMP NOT NULL
                            )
                            " ]))