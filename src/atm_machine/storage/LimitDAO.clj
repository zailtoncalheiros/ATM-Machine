(ns atm-machine.storage.transactionsdao)

(defprotocol LimitDAO
    (update-limit! [storage value]))

(defn create-limit-table! [spec]
  (db/execute! spec [ "CREATE TABLE IF NOT EXISTS public.limit
                            (
                              id BIGSERIAL PRIMARY KEY,
                              account bigint NOT NULL REFERENCES public.person,
                              value bigint NOT NULL,
                              last_update TIMESTAMP NOT NULL
                            )
                            " ]))