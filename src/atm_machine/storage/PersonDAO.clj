(ns atm-machine.storage.persondao)

(defprotocol PersonDAOProtocol
    (put-person! [storage person])
    
    (authentic? [storage agency account password])
    ;; get-id-or-nil returns the account's id whether if the credentials are authentic, otherwise nil.
    (get-id [storage agency account]))


(def person-desc {:table-name "public.person"
                    :id "id"
                    :fullname "fullname"
                    :cpf "cpf"
                    :address "address"
                    :date-of-birth "date_of_birth"
                    :password "password"
                    :agency "agency"
                    :account "account"})

(defrecord PersonDAO [spec]
    PersonDAOProtocol
        (put-person! [storage person]
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
                (db/execute! (:spec storage) sql-query))))

(defn create-person-table! [spec]
  (db/execute! spec [ "CREATE TABLE IF NOT EXISTS public.person
                            (
                              id BIGSERIAL PRIMARY KEY,
                              fullname text NOT NULL,
                              cpf text NOT NULL,
                              address text NOT NULL,
                              date_of_birth text NOT NULL, 
                              password text NOT NULL,

                              agency integer NOT NULL,
                              account integer NOT NULL
                            )
                            " ]))