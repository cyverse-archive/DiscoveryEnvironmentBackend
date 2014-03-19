kameleon
========

Database library that uses Korma to provide access and utilities to the DE's
backend databases.

Extensions to Korma
-------------------

Korma doesn't currently support all of the database features that we need, so
we've added some of the additional features that we need.  All of the
extensions are implemented in kameleon.core.

### Summary ###

```clojure
(use 'korma.core)
(use 'kameleon.core)

;; Entities with many-to-many relationships.
(declare foo bar)

(defentity foo
  (entity-fields :baz)
  (many-to-many bar :foo_bar
                {:lfk :foo_id
                 :rfk :bar_id}))

(defentity bar
  (entity-fields :quux)
  (many-to-many foo :foo_bar
                {:lfk :bar_id
                 :rfk :baz_id}))

;; Retrieving entities in many-to-many relationships.
(select foo
  (kameleon-with bar))

;; Retrieving entities in has-one or belongs-to relationships as separate
;; entities.
(declare blarg glarb)

(defentity blarg
  (entity-fields :field1 :field2)
  (has-one glarb))

(defentity glarb
  (entity-fields :field1 :field2)
  (belongs-to blarg))

(select blarg
  (with-object glarb))
```

### Many-to-Many Relationships ###

Korma doesn't currently support many-to-many relationships, which are used
extensively in the DE's metadata schema, so we needed to add them.  Until the
code to support many-to-many relationships is incorporated into Korma itself,
using them will be a little cumbersome, but it will at least be possible.
Kameleon assumes that many-to-many relationships are implemented using a join
table that contains no fields other than the foreign keys.  In the example
from the summary, the two entity tables are `foo` and `bar`, and the join
table is `foo_bar`.

In queries, it is necessary to use the `kameleon-with` macro instead of
Korma's `with` macro because many-to-many relationships aren't supported by
Korma yet.  The results will look something like this:

```clojure
user=> (pprint (select foo (kameleon-with bar)))
({:id 1,
  :baz "some-value",
  :bar
  [{:id 1,
    :quux "some-other-value"}]})
```

### Retrieving Entities as Separate Objects ###

When retrieving entities in `has-one` and `belongs-to` relationships, Korma
includes the column values of both objects in the same map at the same level.
If there are any duplicate column names then the two columns are distinguished
by appending an underscore and an integer to one or both column names.  If all
columns are being obtained from both entities, however, then it can sometimes
be more convenient to obtain the related entity as a separate object.

To retrieve entities as separate objects in a query, simply use kameleon's
`with-object` macro in the query; the entity definition doesn't have to be
altered.  The results will look something like this:

```clojure
user=> (pprint (select blarg (with-object glarb)))
({:id 1,
  :field1 "some-value",
  :field2 "some-other-value",
  :glarb
  {:id 1,
   :field1 "yet-another-value",
   :field2 "still-another-value"}})
```

Entities
--------

Korma defines entities for most of the actual entities defined in the DE's
metadata schema.  The only exceptions are the entities that use composite
primary keys, which aren't currently supported by Korma.  (It wasn't as
critical to add support for composite primary keys as it was to add support
for many-to-many relationships, so we haven't added support for them yet.)

Attempting to describe each entity here would be a maintenance nightmare.  For
more information on the entities, please see the project's
[documentation page](http://iplantcollaborativeopensource.github.com/kameleon).

License
-------

Copyright (c) 2012, The Arizona Board of Regents on behalf of The University
of Arizona

All rights reserved.

Developed by: iPlant Collaborative at BIO5 at The University of Arizona
http://www.iplantcollaborative.org

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

 * Redistributions of source code must retain the above copyright notice, this
   list of conditions and the following disclaimer.

 * Redistributions in binary form must reproduce the above copyright notice,
   this list of conditions and the following disclaimer in the documentation
   and/or other materials provided with the distribution.

 * Neither the name of the iPlant Collaborative, BIO5, The University of
   Arizona nor the names of its contributors may be used to endorse or promote
   products derived from this software without specific prior written
   permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
