# Balances API

A clojure simple API for bank transactions.

## Getting Started

1. Start the application: `lein run`
2. Go to [localhost:5000](http://localhost:5000/) to see: `Hello World!`
3. Run the app's tests with `lein test`. Read some tests at test/operations/account_service_test.clj.

## Database Configuration

  Define the environment variable DATABASE_URL to use a different postgresql server.

## API Description

**Login**
----
  Log in the system. This action can be performed by a client or an admin.
  It returns a token that will expire with some time.
  The default admin's password is 'secret123'.

* **URL**

  /login

* **Method:**

  `POST`

* **Data Params**

  `{
      "user": [string - optional, it can assume two values: 'client' or 'admin']
      "agency": [integer - optional, only clients have agency],
      "account": [integer - optional, only clients have account],
      "password": [string]"
  }`

* **Example:**

    **Request:** `http://localhost:5000/login`
    **Headers:** `Authorization: Token <some-token>`
    **Data Params:** `{"agency": 5341, "account": 86942, "password": "123"}`
    **Success Response:**
  * **Code:** 200
    `{
      "token": "eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9.eyJ1c2VyIjoiY2xpZW50IiwiYWdlbmN5Ijo1MzQxLCJhY2NvdW50Ijo4Njk0MiwiZXhwIjoxNTA0NTYxNzM0fQ.QXlKhD1-2bY2jk9xv0dD9xuoGdMHE9vhb4UC1lYqx2HdKKdzakmKnIYnpEn8pnAqwX9JC0PsYooFu02G6youYA"
    `}


**Add user**
----
  Add a new user into the system. It can only be performed by the admin.

* **URL**

  /add-user

* **Method:**

  `POST`

* **Headers**

  Authorization: 'Token [token - obtained by login]'

* **Data Params**

  `{
      "fullname": [string],
      "cpf": [string],
      "address": [string],
      "date-of-birth": [string],
      "password": [string]
  }`

* **Example:**

    **Request:** `http://localhost:5000/add-user`
    **Headers:** `Authorization: Token <some-token>`
    **Data Params: **
    `{
        "fullname": "Maria Silva",
        "cpf": "012.345.678-9",
        "address": "Rua dos bobos, 0",
        "date-of-birth": "01/01/1990",
        "password": "123456"
    }`
    **Success Response:**
  * **Code:** 200


**Add transaction**
----
  Add a new transaction into the system.

* **URL**

  /transaction

* **Method:**

  `POST`

* **Headers**

  Authorization: 'Token [token - obtained by login]'

* **Data Params**

  `{
	  "description": [string],
	  "value": [integer]
  }`

* **Example:**

    **Request:** `http://localhost:5000/transaction`
    **Headers:** `Authorization: Token <some-token>`
    **Data Params:**
    `{
      "description": "Withdrawal",
      "value": -50
    }`
    **Success Response:**
  * **Code:** 200


**Transfer**
----
  Perform a new transference from a client to another client.

* **URL**

  /transfer

* **Method:**

  `POST`

* **Headers**

  Authorization: 'Token [token - obtained by login]'

* **Data Params**

  `{
	  "agency": [integer],
    "account": [integer],
	  "value": [integer],
  }`

* **Example:**

    **Request:** `http://localhost:5000/transfer`
    **Headers:** `Authorization: Token <some-token>`
    **Data Params:**
    `{
      "agency": 1234,
      "account": 54721,
      "value": 100,
    }`
    **Success Response:**
  * **Code:** 200


**Get Balance**
----
  Returns json data about an account's current balance.

* **URL**

  /balance

* **Headers**

  Authorization: 'Token [token - obtained by login]'

* **Method:**

  `GET`

* **Example:**

    **Request:** `http://localhost:5000/balance`
    **Headers:** `Authorization: Token <some-token>`
    **Success Response:**
  * **Code:** 200
    **Content:** `400`

**Get Statement**
----
  Returns json data about an account's bank statement for a certain period of days.

* **URL**

  /balance

* **Headers**

  Authorization: 'Token [token - obtained by login]'

* **Method:**

  `GET`

* **Example:**

    **Request:** `http://localhost:5000/statement`
    **Headers:** `Authorization: Token <some-token>`
    **Success Response:**
  * **Code:** 200
    **Content:** `[
        {
            "balance": 150,
            "date": "2017-08-30",
            "descriptions": [
                "none 200",
                "Transference to Ag:8952, Acct: 20826 -50"
            ]
        }
    ]`
