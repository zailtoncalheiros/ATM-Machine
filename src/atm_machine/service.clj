(ns atm-machine.service
  (:require [io.pedestal.http :as http]
            [io.pedestal.http.route :as route]
            [io.pedestal.http.body-params :as body-params]
            [ring.util.response :as ring-resp]
            [atm-machine.storage.persondao :as persondao]
            [atm-machine.storage.transactiondao :as transactiondao]
            [atm-machine.storage.dbinit :as dbinit]))


(def spec (dbinit/init-my-db!))

(defn add-user [request]
  (let [user (:json-params request)]
    (persondao/put-person! spec user)
    (ring-resp/created "OK")))

(defn balance [request]
  (let [agency (get-in request [:path-params :agency])
        account (get-in request [:path-params :account])
        password (get-in request [:path-params :password])]
    (if (persondao/authentic? spec agency account password)
      (http/json-response (transactiondao/get-balance spec agency account))
      (ring-resp/status "Not authorized" 401))))

(defn statement [request]
  (let [agency (get-in request [:path-params :agency])
        account (get-in request [:path-params :account])
        password (get-in request [:path-params :password])
        days (get-in request [:query-params :days] 7)]
    (if (persondao/authentic? spec agency account password)
      (http/json-response (transactiondao/get-statement spec agency account days))
      (ring-resp/status "Not authorized" 401))))


(defn add-transaction [request]
  (let [agency (get-in request [:path-params :agency])
        account (get-in request [:path-params :account])
        password (get-in request [:path-params :password])
        operation (get-in request [:json-params])]
    (if (persondao/authentic? spec agency account password)
      (do
        (transactiondao/perform-operation! spec agency account (:value operation) (:description operation))
        (ring-resp/created "OK"))
      (ring-resp/status "Not authorized" 401))))


(defn about-page
  [request]
  (ring-resp/response (format "Clojure %s - served from %s"
                              (clojure-version)
                              (route/url-for ::about-page))))

(defn home-page
  [request]
  (ring-resp/response "Hello World!"))

;; Defines "/" and "/about" routes with their associated :get handlers.
;; The interceptors defined after the verb map (e.g., {:get home-page}
;; apply to / and its children (/about).
(def common-interceptors [(body-params/body-params) http/html-body])

;; Tabular routes
(def routes #{["/" :get (conj common-interceptors `home-page)]
              ["/about" :get (conj common-interceptors `about-page)]
              ["/balance/agency/:agency/account/:account/password/:password" :get (conj common-interceptors `balance)]
              ["/statement/agency/:agency/account/:account/password/:password" :get (conj common-interceptors `statement)]
              ["/transaction/agency/:agency/account/:account/password/:password" :post (conj common-interceptors `add-transaction)]
              ["/add-user" :post (conj common-interceptors `add-user)]})

;; Map-based routes
;(def routes `{"/" {:interceptors [(body-params/body-params) http/html-body]
;                   :get home-page
;                   "/about" {:get about-page}}})

;; Terse/Vector-based routes
;(def routes
;  `[[["/" {:get home-page}
;      ^:interceptors [(body-params/body-params) http/html-body]
;      ["/about" {:get about-page}]]]])


;; Consumed by atm-machine.server/create-server
;; See http/default-interceptors for additional options you can configure
(def service {:env :prod
              ;; You can bring your own non-default interceptors. Make
              ;; sure you include routing and set it up right for
              ;; dev-mode. If you do, many other keys for configuring
              ;; default interceptors will be ignored.
              ;; ::http/interceptors []
              ::http/routes routes

              ;; Uncomment next line to enable CORS support, add
              ;; string(s) specifying scheme, host and port for
              ;; allowed source(s):
              ;;
              ;; "http://localhost:8080"
              ;;
              ;;::http/allowed-origins ["scheme://host:port"]

              ;; Root for resource interceptor that is available by default.
              ::http/resource-path "/public"

              ;; Either :jetty, :immutant or :tomcat (see comments in project.clj)
              ::http/type :jetty
              ;;::http/host "localhost"
              ::http/port (Integer. (or (System/getenv "PORT") 5000))
              ;; Options to pass to the container (Jetty)
              ::http/container-options {:h2c? true
                                        :h2? false
                                        ;:keystore "test/hp/keystore.jks"
                                        ;:key-password "password"
                                        ;:ssl-port 8443
                                        :ssl? false}})

