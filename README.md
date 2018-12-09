K-anonymisation on streaming data
==================================

Warning
-------

This code is provided as a reference and should not be used with live
data. Contrary to the name, anonymised datasets can be de-anonymised with
additional information that is usually available from outside of the data set.

For example, researchers were able to link public IMDB reviews with anonmised
Netflix viewings and thereby learn other viewing history that the viewers would
prefer to have left private:

https://www.cs.cornell.edu/~shmat/shmat_oak08netflix.pdf

Aside from revealing viewing history of users without their consent, this led
to lawsuits against Netflix, which Netflix settled outside of court.

Please see Differential Privacy and Chrome's RAPPOR for more robust methods to
anonymise data.

Step 1
------

Since this is a streaming interface anonymising data, consumers can expect from
it these properties:

1) Stream-Contract: Throughput capability as high as possible, if not exceeding
the source
2) Data-Contract: There is an expectation of some inaccuracy (but should be
minimised within other contraints)
3) Stream-Contract: A SLA of latency introduced by the stream to elements

I've chosen to implement the FADS algorithm: "Fast clustering-based
anonymization approaches with time constraints for data streams" by Kun Guo,
Qishan Zhang

The algorithm places a constant delay on each record to give predictable
latency performance on the stream, and the info loss of the record when
published follows from this - the longer the delay (and in real life the higher
the throughput) gives more buffered data for finding a good generalisation.

Caveats:
Each unique record should only be processed once for the anonymity property
to prevail.
There may still be unpublished data buffered when the stream source stops
processing data.

Step 2
------

For simplicity I've used a relative distance metric and relative info loss
throughout, which is sufficient for internal comparisons on a single value.
Further improvements would be to generalise the code to allow multiple
Quasi-Identifiers, including a distance calculation for non-continuous
(categorical) data, and the Euclidean distance metric.

In the academic literature an info loss measure looks at the size of the
cluster range in relation to the domain range of the value, however to measure
accuracy more directly I've used the actual information loss calculating the
distance of the anonymised value with the published record raw value. This
ensures that the closest possible anonymised value is selected for each
published record of those available, instead of taking the central value from a
cluster with smallest range.

Then average difference for raw vs. anonymised is then compared with the range
of domain values seen as in the usual definition of info loss ratio.

Step 3
------

This part can be tried by tweaking the delayConstraint parameter - larger
values will give lower info loss but extra delay, and smaller values will
output results with less delay, but will consequently have no option but create
less accurate clusters.

Next steps
----------

1. Finish tests, making sure there is full code coverage, correct results are
returned (as per the algorithm), and that edge cases are covered.

2. The concept of time and incoming record can be decoupled so that varying
rates of input can arrive. As with other implementations, if <k elements arrive
within the latency window and no existing cluster is suitable for one of those
records, then we'll need to wait for at least k elements to arrive for creation
of a new cluster.

3. This algorithm roughly has two parts -

a) searching and reusing good cached clusters, and
b) creation of even better clusters by searching for nearest neighbours.

Both of these operations are robust to imperfect results (it's permissible to
return a slightly less than optimal result, so long as it is indeed
k-anonymised), which helps to keep the design space flexible. They can be
implemented linearly in stream size (see
https://stats.stackexchange.com/questions/219655/k-nn-computational-complexity
for cluster creation KNN complexity) and a limit already exists on both cluster
cache size and records buffer size which can be reduced for computation
reasons.

It's also possible to implement both in a highly parallel fashion by first
mapping the loss or distance function in parallel across the dataset and then
performing a reduce operation to aggregate the top result or top k results
respectively - whether it be on a single multi-core host, local HPC cluster, or
across the network (each introducing additional latency).

It seems that the records buffer size will likely be bound more from the
latency introduced by buffering than computationally, however it seems that the
cached cluster search could indeed benefit from one or more of the above
parallel approaches.  ie. when a record first enters the buffer, we might fire
off a network search for a good previously known cluster, which then streams
back good clusters as it finds them. By the time that record has reached the
latency cap there should hopefully be a good candidate that has arrived from
the network, and then as normal a local host cluster search is performed
(better clusters might have been produced which the network did not know about
yet), with all 3 candidate clusters (network cached cluster, local cached
cluster, and newly created cluster) being compared to pick the best option.  A
network option would also allow storage of more cached clusters than a local
option would allow.

4. It is worth considering starting the stream with a larger delay constraint,
because we are short of good cached clusters initially, and entirely dependent
on making good new ones for low info loss. This problem also recurs if the
underlying distribution of data in the stream changes from what was received
previously.

Ideally delayConstraint should be dynamically calculated in order to minimise
some cost function of the info loss ratio metric and latency metric. Both of
those metrics in turn are not quite ideal being simple mean averaging, since
for long running streams they will tend to be weighted towards the past.
Something like a moving average is more sensible for those.

A kind of trend analysis for the cost function could be used, so that if the
cost function is showing a clear trend of rising, then we assume latency is now
the significant factor and it is time to publish even with poor info loss.
