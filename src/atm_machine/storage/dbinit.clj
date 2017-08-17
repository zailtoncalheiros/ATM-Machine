(ns atm-machine.storage.dbinit
    (:require [atm-machine.storage.persondao :as persondao]
              [atm-machine.storage.limitdao :as limitdao]
              [atm-machine.storage.transactiondao :as transactiondao]))

(def spec (or (System/getenv "DATABASE_URL")
              "postgresql://zailton:teste123@localhost:5432/zailton"))

(defn init-my-db! []
    (persondao/create-person-table! spec)
    (transactiondao/create-transaction-table! spec)
    (limitdao/create-limit-table! spec)
    spec)
