## Zen-Schema - Zchema

* Zchema is optional!
* Zchema is open!


* Zchema has global yet simple namespace like java


### Data ownership view

We have data producer and data consumer.
Sometimes consumer force producer to conform to a schema (record data to the server).
Sometimes producers just provides schema as a fact (read data from the server).

We can introduce data ownership - the owner of data defines schemas.
You can transfer ownership (record data to the server)
Or can borrow data (read data from the server)

Good data owner provides policies for data - schemas.
Bad may not - just give you data (say thank you!)
    
### Multiple-inheritance problem

fhir.Resource -> fhir.DomainResoure -> fhir.Patient        ->   us.Patient
id/rt       narrative        name:HumanName      race

Patient::Resource
Patient is-a Resource


resourceType: fhir.Patient
profile: [US.Patient]


resourceType: US.Patient (is Patient isa DomRes isa Res)


Data each chunk has only one type. 

type is named link to schema
type is a metadata


_type: US.Patient
name: ...
race: ...







