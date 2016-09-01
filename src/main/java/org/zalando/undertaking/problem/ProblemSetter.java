package org.zalando.undertaking.problem;

/**
 * Used to record information about a certain problem that occurred during the processing of a HTTP request, so that
 * this information can be sent to clients.
 *
 * @see  <a href="https://tools.ietf.org/html/draft-nottingham-http-problem-07">ProblemSetter Details for HTTP APIs</a>
 */
public interface ProblemSetter<S extends ProblemSetter<S>> {

    /**
     * Sets the absolute URI that identifies the problem type. When dereferenced, it SHOULD provide human-readable
     * documentation for the problem type (e.g., using HTML).
     *
     * @param   type  an absolute URI that identifies the problem type
     *
     * @return  this problem setter*
     *
     * @throws  NullPointerException  if {@code type} is {@code null}
     */
    default S setType(final String type) {
        return setParameter("type", type);
    }

    /**
     * Sets a short, human-readable summary of the problem type. It SHOULD NOT change from occurrence to occurrence of
     * the problem, except for purposes of localization.
     *
     * @param   title  the short, human-readable summary of the problem type
     *
     * @return  this problem setter
     *
     * @throws  NullPointerException  if {@code title} is {@code null}
     */
    default S setTitle(final String title) {
        return setParameter("title", title);
    }

    /**
     * Sets a human-readable explanation specific to the occurrence of the problem.
     *
     * @param   detail  the human readable explanation of the problem
     *
     * @return  this problem setter
     *
     * @throws  NullPointerException  if {@code detail} is {@code null}
     */
    default S setDetail(final String detail) {
        return setParameter("detail", detail);
    }

    /**
     * Sets an absolute URI that identifies the specific occurrence of the problem. It may or may not yield further
     * information if dereferenced.
     *
     * @param   instance  the absolute URI that identifies the specific occurrence of the problem
     *
     * @return  this problem setter
     *
     * @throws  NullPointerException  if {@code instance} is {@code null}
     */
    default S setInstance(final String instance) {
        return setParameter("instance", instance);
    }

    /**
     * Sets an arbitrary textual parameter.
     *
     * @param   name   parameter name
     * @param   value  parameter value
     *
     * @return  this problem setter
     *
     * @throws  NullPointerException  if one of the parameters is {@code null}
     */
    S setParameter(String name, String value);

    /**
     * Sets the {@code Throwable} instance that triggered the problem.
     *
     * @param   error  the {@code Throwable} that triggered the problem
     *
     * @return  this problem setter
     *
     * @throws  NullPointerException  if {@code error} is {@code null}
     */
    S setError(Throwable error);

}
