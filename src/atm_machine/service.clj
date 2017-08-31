(ns atm-machine.service
  (:require [io.pedestal.http :as http]
            [io.pedestal.http.route :as route]
            [io.pedestal.http.body-params :as body-params]
            [ring.util.response :as ring-resp]
            [atm-machine.storage.persondao :as persondao]
            [atm-machine.storage.transactiondao :as transactiondao]
            [atm-machine.storage.dbinit :as dbinit]

            [io.pedestal.interceptor.helpers :refer (defmiddleware)]

            [clj-time.core :as time]
            [buddy.sign.jwt :as jwt]
            [buddy.auth :refer [authenticated? throw-unauthorized]]
            [buddy.auth.backends.token :refer [jws-backend]]
            [buddy.auth.middleware :refer [authentication-request]]))


(def spec (dbinit/init-my-db!))


(def client "client")
(def admin-password "secret123")

(defn client? [user]
  (= user "client"))
(defn admin? [user]
  (= user "admin"))
(defn valid-admin? [user password]
  (and (admin? user) (= password admin-password)))


(def secret "mysupersecret")

(def auth-backend (jws-backend {:secret secret :options {:alg :hs512}}))

(defmiddleware middleware-interceptor
  ([request] (authentication-request request auth-backend))
  ([response] response))

(defn generateToken [claims]
  (jwt/sign claims secret {:alg :hs512}))

(defn login [request]
  (let [user (get-in request [:json-params :user] client)
        agency (get-in request [:json-params :agency] 0)
        account (get-in request [:json-params :account] 0)
        password (get-in request [:json-params :password])
        claims {:user user
                :agency agency
                :account account
                :exp (time/plus (time/now) (time/seconds 360000))}]
    (if (client? user)
      (if (persondao/authentic? spec agency account password)
        (http/json-response {:token (generateToken claims)})
        (throw-unauthorized))
      (if (valid-admin? user password)
        (http/json-response {:token (generateToken claims)})
        (throw-unauthorized)))))


(defn add-user [request]
  (prn request)
  (if-not (authenticated? request)
    (throw-unauthorized)
    (let [user (:json-params request)
          logged-user (get-in request [:identity :user])]
      (if-not (admin? logged-user)
        (throw-unauthorized)
        (do (persondao/put-person! spec user)
          (ring-resp/created "OK"))))))

(defn balance [request]
  (if-not (authenticated? request)
    (throw-unauthorized)
    (let [logged-user (get-in request [:identity :user])]
      (if-not (client? logged-user)
        (throw-unauthorized)
        (let [agency (get-in request [:identity :agency])
              account (get-in request [:identity :account])]
          (http/json-response (transactiondao/get-balance spec agency account)))))))


(defn statement [request]
  (if-not (authenticated? request)
    (throw-unauthorized)
    (let [logged-user (get-in request [:identity :user])]
      (if-not (client? logged-user)
        (throw-unauthorized)
        (let [agency (get-in request [:identity :agency])
              account (get-in request [:identity :account])
              days (get-in request [:query-params :days] 7)]
            (http/json-response (transactiondao/get-statement spec agency account days)))))))


(defn add-transaction [request]
  (if-not (authenticated? request)
    (throw-unauthorized)
    (let [logged-user (get-in request [:identity :user])]
      (if-not (client? logged-user)
        (throw-unauthorized)
        (let [agency (get-in request [:identity :agency])
              account (get-in request [:identity :account])
              operation (get-in request [:json-params])]
            (do
              (transactiondao/perform-operation! spec agency account (:value operation) (:description operation))
              (ring-resp/created "OK")))))))

(defn perform-transfer [request]
  (if-not (authenticated? request)
    (throw-unauthorized)
    (let [logged-user (get-in request [:identity :user])]
      (if-not (client? logged-user)
        (throw-unauthorized)
        (let [agency (get-in request [:identity :agency])
              account (get-in request [:identity :account])
              operation (get-in request [:json-params])]
            (do
              (transactiondao/transfer! spec agency account (:agency operation) (:account operation) (:value operation))
              (ring-resp/created "OK")))))))

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
(def common-interceptors [middleware-interceptor (body-params/body-params) http/html-body])

;; Tabular routes
(def routes #{["/" :get (conj common-interceptors `home-page)]
              ["/login" :post (conj common-interceptors `login)]
              ["/about" :get (conj common-interceptors `about-page)]
              ["/balance" :get (conj common-interceptors `balance)]
              ["/statement" :get (conj common-interceptors `statement)]
              ["/transaction" :post (conj common-interceptors `add-transaction)]
              ["/transfer" :post (conj common-interceptors `perform-transfer)]
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

