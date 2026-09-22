package dev.sosea1.retropolymorph.compat;

import java.util.Objects;

/** Metadata for one built-in or optional integration. */
public final class IntegrationDescriptor {

    private final String id;
    private final String displayName;
    private final int defaultPriority;

    public IntegrationDescriptor(String id, String displayName, int defaultPriority) {
        if (id == null) {
            throw new NullPointerException("id");
        }
        if (displayName == null) {
            throw new NullPointerException("displayName");
        }
        this.id = id;
        this.displayName = displayName;
        this.defaultPriority = defaultPriority;
    }

    public String getId() {
        return this.id;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public int getDefaultPriority() {
        return this.defaultPriority;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IntegrationDescriptor that = (IntegrationDescriptor) o;
        return this.defaultPriority == that.defaultPriority
                && this.id.equals(that.id)
                && this.displayName.equals(that.displayName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id, this.displayName, Integer.valueOf(this.defaultPriority));
    }

    @Override
    public String toString() {
        return "IntegrationDescriptor{" +
                "id='" + this.id + '\'' +
                ", displayName='" + this.displayName + '\'' +
                ", defaultPriority=" + this.defaultPriority +
                '}';
    }
}
