# Welcome to the CAP SFLIGHT App

Version of SFlight sample app to experiment with getting data
for some master data tables from ABAP SQL service.

## Deploy to CF

Ensure that there is no `.cdsrc-private.json` in the root directory of the project.

Prepare/Deploy/Undeploy:
```
> npm install
> mbt build
> cf deploy mta_archives\capire.sflight_1.0.0.mtar
> cf undeploy capire.sflight --delete-services --delete-service-keys -f
```

## Hybrid

### Build for HANA

```
cds build --for hana
```

### Deploy to HANA

Ensure to be logged in to cf (`cf login`) in the correct org/space.

Before deployment: grant access to synonyms to OO user of HDI container, otherwise deployment fails (see below):
```sql
GRANT LINKED DATABASE ON REMOTE SOURCE CAP_PRF TO "<schema-name>#OO" WITH GRANT OPTION;
```

Deploy the model to HANA with
```
cds deploy --to hana
```

### Run Node App

```
cds watch --profile hybrid
``` 

### Bind to existing container

Retrieve binding information for HDI container that you didn't deploy yourself:
```
cds bind -2 <hdi-instance>:<service-key>
```

`hdi-instance` is the name of the instance of the hdi-shared service  
`service-key` is the name of the corresponding service key.  
Example: `cds bind -2 db-sflight:db-sflight-key`.

Get credentials ...
```
cds env get requires.db.credentials --profile hybrid --resolve-bindings
```

More info: [capire/advanced/hybrid-testing#services-on-cloud-foundry](https://pages.github.tools.sap/cap/docs/advanced/hybrid-testing#services-on-cloud-foundry).



## Prepare HANA

### Access to other schema on same HANA

Use a synonym to access a table in another schema of the same HANA.
Kind of exercise for accessing the SQL service of an ABAP system.

First create the other schema/table and grant access:
```sql
create schema external_data;
create column table external_data.TravelAgency (
  AgencyID NVARCHAR(6) NOT NULL,
  Name NVARCHAR(80),
  Street NVARCHAR(60),
  PostalCode NVARCHAR(10),
  City NVARCHAR(40),
  CountryCode_code NVARCHAR(3),
  PhoneNumber NVARCHAR(30),
  EMailAddress NVARCHAR(256),
  WebAddress NVARCHAR(256),
  PRIMARY KEY(AgencyID)
);
insert into external_data.TravelAgency values ('070002','Fly High','Berliner Allee 11','40880','Duesseldorf','DE','+49 2102 69555',
                                               'info@flyhigh.sap','http://www.flyhigh.sap');
select * from external_data.TravelAgency;
GRANT SELECT ON external_data.TravelAgency TO "C5BE2178A3834608A97D8F6EF34520F9#OO" WITH GRANT OPTION;
```

Synonym definition in `db/src/ext-synonyms.hdbsynonym`:
```jsonc
  "SAP_FE_CAP_TRAVEL_TRAVELAGENCY" : {
     "target" : {
        "schema" : "EXTERNAL_DATA",
        "object" : "TRAVELAGENCY"
     }
  },
```

Without the GRANT we get the deployment error:
> The container's object owner "C5BE2178A3834608A97D8F6EF34520F9#OO" is not authorized to access the "EXTERNAL_DATA.TRAVELAGENCY" synonym target.
This user needs to be granted "SELECT" ("EXECUTE" for procedures) privileges on the target object.



### Access to SQL Service on ABAP system

PSE: Personal Security Environment
(could be regarded as certificate collection)


Create a PSE, create a certificate, and add the certificate to the PSE:
```sql
DROP REMOTE SOURCE CAP_PRF CASCADE;
DROP PSE CAP_PRF_TRUST_PSE CASCADE;
DROP CERTIFICATE CAP_PRF_TRUST_PSE_CERT;

CREATE PSE CAP_PRF_TRUST_PSE;
CREATE CERTIFICATE CAP_PRF_TRUST_PSE_CERT FROM '
-----BEGIN CERTIFICATE-----
MIIDrzCCApegAwIBAgIQCDvgVpBCRrGhdWrJWZHHSjANBgkqhkiG9w0BAQUFADBh
MQswCQYDVQQGEwJVUzEVMBMGA1UEChMMRGlnaUNlcnQgSW5jMRkwFwYDVQQLExB3
d3cuZGlnaWNlcnQuY29tMSAwHgYDVQQDExdEaWdpQ2VydCBHbG9iYWwgUm9vdCBD
QTAeFw0wNjExMTAwMDAwMDBaFw0zMTExMTAwMDAwMDBaMGExCzAJBgNVBAYTAlVT
MRUwEwYDVQQKEwxEaWdpQ2VydCBJbmMxGTAXBgNVBAsTEHd3dy5kaWdpY2VydC5j
b20xIDAeBgNVBAMTF0RpZ2lDZXJ0IEdsb2JhbCBSb290IENBMIIBIjANBgkqhkiG
9w0BAQEFAAOCAQ8AMIIBCgKCAQEA4jvhEXLeqKTTo1eqUKKPC3eQyaKl7hLOllsB
CSDMAZOnTjC3U/dDxGkAV53ijSLdhwZAAIEJzs4bg7/fzTtxRuLWZscFs3YnFo97
nh6Vfe63SKMI2tavegw5BmV/Sl0fvBf4q77uKNd0f3p4mVmFaG5cIzJLv07A6Fpt
43C/dxC//AH2hdmoRBBYMql1GNXRor5H4idq9Joz+EkIYIvUX7Q6hL+hqkpMfT7P
T19sdl6gSzeRntwi5m3OFBqOasv+zbMUZBfHWymeMr/y7vrTC0LUq7dBMtoM1O/4
gdW7jVg/tRvoSSiicNoxBN33shbyTApOB6jtSj1etX+jkMOvJwIDAQABo2MwYTAO
BgNVHQ8BAf8EBAMCAYYwDwYDVR0TAQH/BAUwAwEB/zAdBgNVHQ4EFgQUA95QNVbR
TLtm8KPiGxvDl7I90VUwHwYDVR0jBBgwFoAUA95QNVbRTLtm8KPiGxvDl7I90VUw
DQYJKoZIhvcNAQEFBQADggEBAMucN6pIExIK+t1EnE9SsPTfrgT1eXkIoyQY/Esr
hMAtudXH/vTBH1jLuG2cenTnmCmrEbXjcKChzUyImZOMkXDiqw8cvpOp/2PV5Adg
06O/nVsJ8dWO41P0jmP6P6fbtGbfYmbW0W5BjfIttep3Sp+dWOIrWcBAI+0tKIJF
PnlUkiaY4IBIqDfv8NZ5YBberOgOzW6sRBc4L0na4UU+Krk2U886UAb3LujEV0ls
YSEY1QSteDwsOoBrp+uvFRTp2InBuThs4pFsiv9kuXclVzDAGySj4dzp30d8tbQk
CAUw7C29C79Fv1C5qfPrmAESrciIxpg0X40KPMbp1ZWVbd4=
-----END CERTIFICATE-----
';
ALTER PSE CAP_PRF_TRUST_PSE ADD CERTIFICATE CAP_PRF_TRUST_PSE_CERT;
```

Create the remote source and link to the PSE:
```sql
CREATE REMOTE SOURCE CAP_PRF
ADAPTER abapodbc
CONFIGURATION 'UidType=alias;driver=ODBC_driver_for_ABAP.so;servicepath=/sap/bc/sql/sql1/sap/s_privileged;host=25638c75-a54b-4658-8b04-3a1156f2c4f5.abap.eu10.hana.ondemand.com;port=443;language=EN;typemap=semantic'
WITH CREDENTIAL TYPE 'PASSWORD' USING 'user=ITAPC1_SQL_CAP_TESTS;password=du2wPBiDdYbRbvvyGliPedKLvNHuWX_hzAvVNEH';

SET PSE CAP_PRF_TRUST_PSE PURPOSE REMOTE SOURCE FOR REMOTE SOURCE CAP_PRF;
```

Creating the remote source only works with a running script server. Otherwise we get the error:
```
> Could not execute 'CREATE REMOTE SOURCE CAP_PRF ADAPTER abapodbc CONFIGURATION ...'
Error: (dberror) [403]: internal error: The scriptserver isn't activated, ABAPODBC adapter should run in safe mode: line 59 col 21 (at pos 21)
```

Test Queries:
```sql
SELECT * FROM CAP_PRF.SYS.VIEWS;
SELECT * FROM CAP_PRF."/ITAPC1/SQL_FLIGHTS_1"."Airline";
SELECT * FROM CAP_PRF."/ITAPC1/SQL_FLIGHTS_1"."TravelAgency";
```

Note: apparently names must be quoted in queries:
```sql
SELECT "AgencyID" FROM CAP_PRF."/ITAPC1/SQL_FLIGHTS_1"."TravelAgency"; -- ok
SELECT AgencyID FROM CAP_PRF."/ITAPC1/SQL_FLIGHTS_1".TravelAgency; -- error
-- Could not execute 'SELECT AgencyID FROM CAP_PRF."/ITAPC1/SQL_FLIGHTS_1".TravelAgency'
-- Error: (dberror) [476]: invalid remote object name: Unable to retrieve remote metadata for <NULL>./ITAPC1/SQL_FLIGHTS_1.TRAVELAGENCY
```

The OO user of the HDI container needs access to the remote data source,
otherwise the deplyoment of the synonym fails.
```sql
GRANT LINKED DATABASE ON REMOTE SOURCE CAP_PRF TO "C5BE2178A3834608A97D8F6EF34520F9#OO" WITH GRANT OPTION;
```
(of course only works once there is a HDI container -> HANA deployment must have been deone before w/o acess
to ABAP tables)


## Use external tables in CAP

### Synonyms 

Define synonyms in `db/src/abap-synonyms.hdbsynonym`. They map the ABAP tables from the
remote source into our HDI container and look like this:
```jsonc
{ 
  "TravelAgency_ABAP" : {
     "target" : { 
        "database" : "CAP_PRF",
        "schema"   : "/ITAPC1/SQL_FLIGHTS_1",
        "object"   : "TravelAgency"
     }
  }
}
```

### Mapping views

On top of the synonyms there are mapping views that handle the upper/lower case mismatch.
Definitions in `db/src/Map_xyz.hdbview` look like
```sql
VIEW ext_abap_TravelAgency AS SELECT
  "AgencyID"         AS AgencyID,
  "Name"             AS Name,
  "Street"           AS Street,
  "PostalCode"       AS PostalCode,
  "City"             AS City,
  "CountryCode_code" AS CountryCode_code,
  "PhoneNumber"      AS PhoneNumber,
  "EMailAddress"     AS EMailAddress,
  "WebAddress"       AS WebAddress
FROM "TravelAgency_ABAP"
```

### Entity definitions

The mapping views are made known to CDS in `db/ext-abap.cds` via entities with `@cds.persistence.exists`.
Like this:
```cds
namespace ext.abap;

@cds.persistence.exists
entity TravelAgency {
  key AgencyID : String(6);
  Name         : String(80);
  Street       : String(60);
  PostalCode   : String(10);
  City         : String(40);
  //CountryCode  : Country;
  CountryCode_code : String(3);
  PhoneNumber  : String(30);
  EMailAddress : String(256);
  WebAddress   : String(256);
};
```

Then remove the definition of these entities from `db/master--data.cds`,
keep the annotations, and adapt all references to these entities (e.g. in `app/labells.cds`).

### Runtime problem

Node runtime sends a SQL statement for a count query that ABAP can't handle. Error:
```
[cds] - [SqlError: internal error: Error opening the cursor for the remote database <CAP_PRF> [SAP][ODBCforABAP] (42000) [SELECT:901]It is not possible to derive a type for placeholder number "0" from its context.: line 0 col 0
 in statement SELECT COUNT( ? ) FROM "/ITAPC1/SQL_FLIGHTS_1"."TravelAgency" "TravelAgency_ABAP"
] {
  code: 403,
  sqlState: 'HY000',
  level: 1,
  position: 0,
  query: 'SELECT count ( ? ) AS "$count" FROM TravelService_TravelAgency ALIAS_1',
  values: [ 1 ],
  id: '1528433',
  timestamp: 1708090478237
}
```

Local fix in `node_modules/@sap/cds/libx/_runtime/db/query/read.js` line 29, function `_createCountQuery`
(https://github.tools.sap/cap/cds/blob/402490ff788df3dbb4bd3f53677b0d55f0bf1b8e/libx/_runtime/db/query/read.js#L15-L32).
Change to
```
  _query.SELECT.columns = [{ func: 'count', args: ['*'], as: '$count' }]
```

## Use CSN exposure service (?)

CSN Exposure OData V4:
https://25638c75-a54b-4658-8b04-3a1156f2c4f5.abap.eu10.hana.ondemand.com/sap/opu/odata4/sap/csn_exposure_v4/srvd_a2x/sap/csn_exposure/0001/
 
ESH Search OData V2:
https://25638c75-a54b-4658-8b04-3a1156f2c4f5.abap.eu10.hana.ondemand.com/sap/opu/odata/sap/ESH_SEARCH_SRV

Logon with the user/pw given above in the creation of the remote source:
* user=ITAPC1_SQL_CAP_TESTS
* password=du2wPBiDdYbRbvvyGliPedKLvNHuWX_hzAvVNEH

Wiki on CSN Exposure Service: https://wiki.one.int.sap/wiki/display/ApplServ/CSN+-+Service+Implementation




