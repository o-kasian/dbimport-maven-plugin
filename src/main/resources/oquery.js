//css3 attribute selectors for deep JS objects, by dandavis 2013. [CC BY] - v1.12


//compat pack for Array methods and Object.keys in older browsers:
(function() {
    var $ = "length", B = "slice", e = Array.prototype, t, n, r = {
        map: function(e, t) {
            var n = this[B](), r = n[$], i = 0, s = [];
            for (; i < r; i++)
                i in n && (s[i] = e.call(t, n[i], i, n));
            return s
        },
        filter: function(e, t) {
            var n = this[B](), r = n[$], i = 0, s = [], o = 0;
            for (; i < r; i++)
                i in n && e.call(t, n[i], i, n) && (s[o++] = n[i]);
            return s
        },
        every: function(e, t) {
            var n = this[B](), r = n[$], i = 0;
            return r && n.filter(e, t)[$] == r
        },
        indexOf: function(e, t) {
            var n = this[B](), r = n[$], i = t || 0;
            for (; i < r; i++)
                if (i in n && n[i] === e)
                    return i;
            return-1
        }
    };
    for (t in r)
        n = e[t], e[t] = n || r[t]
})();
Object.keys = Object.keys || function(ob) {
    var r = [], i = 0, z;
    for (z in ob) {
        if (ob.hasOwnProperty(z)) {
            r[i++] = z;
        }
    }
    return r;
};
//end compat pack


function oQuery(obj, cssQuery, select, parent) {

    if (select && !select.join && typeof select === "object") {
        select = Object.keys(select);
    }
    if (select && select.split) {
        select = select.split(/\s*,\s*/);
    }

    var fancy = false, opRx = /[*^$|~!<>]/;
    q = cssQuery.split(/[\[\]#]+/g).filter(Boolean).map(function(a, b, c) {
        var r = a.split('='), op = "";
        this[r[0]] = r[1] || null;
        if ((op = r[0].slice(-1)[0].match(opRx)) || r[1] === undefined) {
            fancy = true;
        }/* end if fancy operator? */
        return this;
    }, {})[0],
            r = [],
            r2 = [],
            props = Object.keys(q),
            ops = props.map(function(aa) {
                return (aa.slice(-1)[0].match(opRx) || [""])[0]
            });

    each(obj, q);

    if (parent) {
        r = r2;
    }

    return select ? r.map(function(a, b) {
        var ob = {};
        select.map(function(aa) {
            ob[aa] = a[aa];
        });
        return ob;
    }) : r;


    function each(obj, q, mom) {
        mom = mom || obj;

        for (var k in obj) {
            if (typeof obj[k] == "object") { //&& !obj[k].join
                each(obj[k], q, obj);
            } else {
                if (r.indexOf(obj) === -1 && (fancy ? props.every(function(aa, b) {
                    var op = ops[b];
                    a = op ? aa.slice(0, -1) : aa;
                    var as = String(obj[a]), p = as.indexOf(q[aa]), qs = String(q[aa]), lc = qs.toLowerCase();
                    switch (op) {
                        case "*":
                            return p !== -1;
                        case "!":
                            return p === -1;
                        case "^":
                            return p === 0;
                        case "<":
                            return as.toLowerCase() <= lc;
                        case ">":
                            return as.toLowerCase() >= lc;

                        case "|":
                            return ("-" + as + "-").replace(/\W/g, "-").indexOf("-" + qs) !== -1;
                        case "~":
                            return (" " + as + " ").replace(/\W/g, " ").indexOf(" " + qs + " ") !== -1;
                        case "$":
                            return as.lastIndexOf(qs) + qs.length === as.length;
                        default:
                            return q[a] === null ? (obj[a] !== undefined) : obj[a] === q[a];
                    }/* end switch(op) */

                }) : props.every(function(a) {
                    return obj[a] == q[a];
                }))) {
                    if (parent) {
                        r2.push(mom);
                    }
                    r.push(obj);
                }/* end if r.indexOf(obj) && fancy ... */
            } //end if object?
        }/* next k in obj */
    }/* end each() */
}/* end oQuery() */

/*  syntax demos:
 
 //query dedmo
 oQuery(o, "[dan]")		//hits an object that has a dan property
 oQuery(o, "[madeupkey]")  	//this should not hit the demo data, an empty array is returned
 oQuery(o, "[dan][role]")	//has dan and role props set to anything excecpt undefined
 oQuery(o, "#dan#role")	//has dan and role props set to anything excecpt undefined
 oQuery(o, "[role=event]")   //role is equal to 'event'
 oQuery(o, "[role^=eve]")	//starts with eve
 oQuery(o, "[role*=eve]")	//contains eve
 oQuery(o, "[role$=eve]")  	//end with eve
 oQuery(o, "[role$=ent]")	//ends with ent
 oQuery(o, "[role$=event]")	//ends with event
 oQuery(o, "[name<=d]" ) 	//name prop starts with a-daa (case insensitive)
 oQuery(o, "[name>=d]" ) 	//name prop starts with d-z  (case insensitive)
 
 
 
 oQuery(o, "[role!=event]") 	//anything but 'event'
 oQuery(o, "[role$=method]") //contains method
 oQuery(o, "[args$=Value]")	//note: hits sub-arrays
 oQuery(o, "[args*=str]")	//note: hits sub-arrays
 oQuery(o, "[args*=strKey]") //note: hits sub-arrays
 oQuery(o, "[args=strKey]")  //note: MISSES sub-arrays containing "strKey"
 
 //custom selection demo:
 oQuery(o, "#dan#name#role", ["name", "fn"] ) 	//has props named dan, name, and role - grab name and fn props. array select
 oQuery(o, "#dan#name#role", "name, fn" ) 		// "" . string select w/space
 oQuery(o, "#dan#name#role", "name,fn" ) 		// "" . string select no space
 oQuery(o, "#dan#name#role", {name:1, fn:1} )	// "" . object select
 
 //parent mode demos:
 oQuery(ob, "[css=dd]", false, true) 			// hits objects containing an object that has a property "css" set to "dd";
 oQuery(ob, "[css=dd]", "title,cat,requires", true)	// hits objects containing an object that has a property "css" set to "dd", custom object response
 
 
 */